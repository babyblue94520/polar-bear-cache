package pers.clare.polarbearcache.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.support.CacheKeyUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class BasicCache implements PolarBearCache {
    private static final Logger log = LogManager.getLogger();

    protected final ConcurrentMap<String, Cache.ValueWrapper> store;
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        return (T) store.computeIfAbsent(String.valueOf(key), (k) -> {
            try {
                return createValueWrapper(valueLoader.call());
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }).get();
    }

    @Override
    public void put(Object key, Object value) {
        String strKey = String.valueOf(key);
        if (!manager.isCacheable()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    store.put(strKey, createValueWrapper(value));
                }
            });
        } else {
            store.put(strKey, createValueWrapper(value));
        }
    }

    @Override
    public Cache.ValueWrapper putIfAbsent(Object key, Object value) {
        return store.putIfAbsent(String.valueOf(key), createValueWrapper(value));
    }

    @Override
    public Cache.ValueWrapper get(Object key) {
        String strKey = String.valueOf(key);
        BasicCacheValueWrapper value = (BasicCacheValueWrapper) this.store.get(strKey);
        if (value != null) {
            if (manager.isCacheable()) {
                if (effectiveTime == 0) return value;
                long now = System.currentTimeMillis();
                if (value.getValidTime() > now) {
                    if (this.extension) value.setValidTime(now + effectiveTime);
                    return value;
                }
            }
            store.remove(strKey);
        }
        return null;
    }

    protected Cache.ValueWrapper createValueWrapper(Object value) {
        return new BasicCacheValueWrapper(value, System.currentTimeMillis() + effectiveTime);
    }

    @NonNull
    @Override
    public void expire(long now) {
        if (effectiveTime == 0) return;
        long oldSize = store.size();
        if (oldSize == 0) return;
        long t = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> ((BasicCacheValueWrapper) entry.getValue()).getValidTime() < now);
        log.debug("{} > {} expire {}ms", oldSize, store.size(), System.currentTimeMillis() - t);
    }

    @Override
    public void evict(Object key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    String str = String.valueOf(key);
                    doEvict(str);
                    evictNotify(str);
                }
            });
        } else {
            String str = String.valueOf(key);
            doEvict(str);
            evictNotify(str);
        }
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
        Pattern pattern = CacheKeyUtil.getPattern(key);
        if (pattern == null) {
            remove(key);
        }else{
            for (Object k : store.keySet()) {
                String keyStr = k.toString();
                if (pattern.matcher(keyStr).find()) {
                    remove(keyStr);
                }
            }
        }
        manager.evictDependents(name, key);
    }

    protected void remove(String key) {
        Object value = null;
        BiFunction<String, Object, Object> evictHandler = manager.getEvictHandler(name);
        if (evictHandler != null) {
            Cache.ValueWrapper wrapper = store.get(key);
            if (wrapper != null) {
                value = wrapper.get();
            }
            value = evictHandler.apply(key, value);
        }
        if (value == null) {
            log.debug("evict remove name:{} key:{}", name, key);
            store.remove(key);
        } else {
            log.debug("evict put name:{} key:{}", name, key);
            store.put(key, createValueWrapper(value));
        }
    }

    @Override
    public void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doClear();
                    manager.clearNotify(name);
                }
            });
        } else {
            doClear();
            manager.clearNotify(name);
        }
    }

    public void onlyClear() {
        doClear();
    }

    private void doClear() {
        BiFunction<String, Object, Object> evictHandler = manager.getEvictHandler(name);
        if (evictHandler == null) {
            store.clear();
        } else {
            for (Iterator<Map.Entry<String, ValueWrapper>> it = store.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ValueWrapper> entry = it.next();
                Object value = evictHandler.apply(entry.getKey(), entry.getValue().get());
                if (value == null) {
                    it.remove();
                } else {
                    entry.setValue(createValueWrapper(value));
                }
            }
        }
        manager.dispatchClear(name);
        manager.clearDependents(name);
        log.debug("clear name:{}", name);
    }
}
