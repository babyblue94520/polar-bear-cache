package pers.clare.polarbearcache.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.support.CacheKeyUtil;
import pers.clare.polarbearcache.support.TransactionSupport;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.regex.Pattern;


public class BasicCache implements PolarBearCache {
    private static final Logger log = LoggerFactory.getLogger(BasicCache.class);

    protected volatile ConcurrentMap<String, Cache.ValueWrapper> store;
    protected final BasicCacheManager manager;
    protected final String name;
    protected final long effectiveTime;
    protected final boolean extension;

    protected BasicCache(
            BasicCacheManager manager
            , String name
            , long effectiveTime
            , boolean extension
    ) {
        this(manager, name, new ConcurrentHashMap<>(), effectiveTime, extension);
    }

    protected BasicCache(
            BasicCacheManager manager
            , String name
            , ConcurrentMap<String, Cache.ValueWrapper> store
            , long effectiveTime
            , boolean extension
    ) {
        this.store = store;
        this.name = name;
        this.manager = manager;
        this.effectiveTime = effectiveTime;
        this.extension = extension;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return Collections.unmodifiableMap(store);
    }

    void discardStorage() {
        store = new ConcurrentHashMap<>();
    }

    @Override
    public Object getValue(String key) {
        Cache.ValueWrapper value = get(key);
        if (value == null) return null;
        return value.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        return (T) getValue(String.valueOf(key));
    }

    /**
     * Loads atomically and caches immediately, including inside a transaction.
     * Callers must avoid caching uncommitted data; rollback does not undo this load.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        String strKey = String.valueOf(key);
        if (!manager.isCacheable()) {
            store.remove(strKey);
            return loadValue(key, valueLoader);
        }
        return (T) store.compute(strKey, (k, current) -> {
            BasicCacheValueWrapper value = getValidValue(current);
            if (value != null) return value;
            return createValueWrapper(loadValue(key, valueLoader));
        }).get();
    }

    @Override
    public Cache.ValueWrapper get(Object key) {
        String strKey = String.valueOf(key);
        if (!manager.isCacheable()) {
            store.remove(strKey);
            return null;
        }
        return getValidValue(strKey);
    }

    private BasicCacheValueWrapper getValidValue(String key) {
        AtomicReference<BasicCacheValueWrapper> result = new AtomicReference<>();
        store.computeIfPresent(key, (k, current) -> {
            BasicCacheValueWrapper value = getValidValue(current);
            result.set(value);
            return value;
        });
        return result.get();
    }

    private BasicCacheValueWrapper getValidValue(Cache.ValueWrapper current) {
        if (!(current instanceof BasicCacheValueWrapper)) return null;
        BasicCacheValueWrapper value = (BasicCacheValueWrapper) current;
        if (effectiveTime == 0) return value;

        long now = System.currentTimeMillis();
        if (value.getValidTime() <= now) return null;
        if (extension) value.setValidTime(now + effectiveTime);
        return value;
    }

    private <T> T loadValue(Object key, Callable<T> valueLoader) {
        try {
            return valueLoader.call();
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        String strKey = String.valueOf(key);
        if (!manager.isCacheable()) return;
        ConcurrentMap<String, Cache.ValueWrapper> targetStore = store;
        TransactionSupport.afterCommit(() -> targetStore.put(strKey, createValueWrapper(value)));
    }

    @Override
    public Cache.ValueWrapper putIfAbsent(Object key, Object value) {
        String strKey = String.valueOf(key);
        if (!manager.isCacheable()) {
            store.remove(strKey);
            return null;
        }

        BasicCacheValueWrapper current = getValidValue(strKey);
        if (current != null) return current;

        AtomicReference<Cache.ValueWrapper> existing = new AtomicReference<>();
        ConcurrentMap<String, Cache.ValueWrapper> targetStore = store;
        TransactionSupport.afterCommit(() -> targetStore.compute(strKey, (k, wrapper) -> {
            BasicCacheValueWrapper valid = getValidValue(wrapper);
            if (valid != null) {
                existing.set(valid);
                return valid;
            }
            return createValueWrapper(value);
        }));
        return existing.get();
    }


    protected Cache.ValueWrapper createValueWrapper(Object value) {
        return new BasicCacheValueWrapper(value, System.currentTimeMillis() + effectiveTime);
    }

    @Override
    public void putNotify(String key) {
        if (key == null) return;
        TransactionSupport.afterCommit(() -> {
            manager.evictDependents(name, key);
            manager.evictNotify(name, key);
        });
    }

    @Override
    public void expire(long now) {
        if (effectiveTime == 0) return;
        if (store.isEmpty()) return;
        long oldSize = store.size();
        long t = System.currentTimeMillis();
        store.forEach((key, ignored) -> store.computeIfPresent(key, (k, current) -> {
            if (!(current instanceof BasicCacheValueWrapper)) return null;
            return ((BasicCacheValueWrapper) current).getValidTime() <= now ? null : current;
        }));
        log.debug("{} > {} expire {}ms", oldSize, store.size(), System.currentTimeMillis() - t);
    }

    @Override
    public void evict(Object key) {
        String str = String.valueOf(key);
        TransactionSupport.afterCommit(() -> {
            doEvict(str);
            manager.evictDependents(name, str);
            evictNotify(str);
        });
    }

    public void onlyEvict(String key) {
        if (key == null) return;
        doEvict(key);
    }

    public void evictNotify(String key) {
        manager.evictNotify(name, key);
    }

    private void doEvict(String key) {
        log.debug("doEvict name:{} key:{}", name, key);
        ConcurrentMap<String, Cache.ValueWrapper> targetStore = store;
        Pattern pattern = CacheKeyUtil.getPattern(key);
        if (pattern == null) {
            remove(targetStore, key);
        }else{
            for (Object k : targetStore.keySet()) {
                String keyStr = k.toString();
                if (pattern.matcher(keyStr).find()) {
                    remove(targetStore, keyStr);
                }
            }
        }
    }

    private void remove(ConcurrentMap<String, Cache.ValueWrapper> targetStore, String key) {
        BiFunction<String, Object, Object> evictHandler = manager.getEvictHandler(name);
        if (evictHandler == null) {
            targetStore.remove(key);
            return;
        }
        reload(targetStore, key, targetStore.get(key), evictHandler);
    }

    private void reload(
            ConcurrentMap<String, Cache.ValueWrapper> targetStore, String key,
            Cache.ValueWrapper expected, BiFunction<String, Object, Object> handler
    ) {
        Cache.ValueWrapper replacement = null;
        try {
            Object value = handler.apply(key, expected == null ? null : expected.get());
            if (value != null) replacement = createValueWrapper(value);
        } catch (Exception e) {
            log.warn("Cache reload failed, invalidating {} key:{}", name, key, e);
        }
        // Apply only to the observed entry in the original storage generation.
        if (replacement == null) {
            if (expected != null) targetStore.remove(key, expected);
        } else if (expected == null) {
            targetStore.putIfAbsent(key, replacement);
        } else {
            targetStore.replace(key, expected, replacement);
        }
    }

    @Override
    public void clear() {
        TransactionSupport.afterCommit(() -> {
            doClear();
            manager.clearDependents(name);
            manager.clearNotify(name);
        });
    }

    public void onlyClear() {
        doClear();
    }

    private void doClear() {
        ConcurrentMap<String, Cache.ValueWrapper> targetStore = store;
        BiFunction<String, Object, Object> evictHandler = manager.getEvictHandler(name);
        if (evictHandler == null) {
            targetStore.clear();
        } else {
            targetStore.forEach((key, expected) -> reload(targetStore, key, expected, evictHandler));
        }
        manager.dispatchClear(name);
        log.debug("clear name:{}", name);
    }

}
