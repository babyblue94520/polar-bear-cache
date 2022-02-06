package pers.clare.polarbearcache.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import pers.clare.polarbearcache.PolarBearCache;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class BasicCache implements PolarBearCache {
    private static final Logger log = LogManager.getLogger();
    private static final char[] regexPrefix = "regex:".toCharArray();
    private static final int regexLength = regexPrefix.length;

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
        this.store = new ConcurrentHashMap<>();
        this.name = name;
        this.manager = manager;
        this.effectiveTime = effectiveTime;
        this.extension = extension;
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
        store.put(String.valueOf(key), createValueWrapper(value));
    }

    @Override
    public Cache.ValueWrapper putIfAbsent(Object key, Object value) {
        return store.putIfAbsent(String.valueOf(key), createValueWrapper(value));
    }

    @Override
    public Cache.ValueWrapper get(Object key) {
        BasicCacheValueWrapper value = (BasicCacheValueWrapper) this.store.get(String.valueOf(key));
        if (value == null) return null;
        if (effectiveTime == 0) return value;
        long now = System.currentTimeMillis();
        if (value.getValidTime() > now) {
            if (this.extension) value.setValidTime(now + effectiveTime);
            return value;
        } else {
            store.remove(key.toString());
            return null;
        }
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
        String str = String.valueOf(key);
        doEvict(str);
        evictNotify(str);
    }

    public void onlyEvict(String key) {
        if (key == null) return;
        doEvict(key);
    }

    public void evictNotify(String key) {
        manager.evictNotify(name, key);
    }

    private void doEvict(String key) {
        String regexKey = toRegexKey(key);
        if (regexKey == null) {
            remove(key);
        } else {
            Pattern pattern = Pattern.compile(regexKey);
            String str;
            for (Object k : store.keySet()) {
                str = k.toString();
                if (pattern.matcher(str).find()) {
                    remove(str);
                }
            }
        }
    }

    protected void remove(String key) {
        BiFunction<String, Object, Object> evictHandler = manager.getEvictHandler(name);
        if (evictHandler == null) {
            store.remove(key);
            log.debug("evict name:{} key:{}", name, key);
        } else {
            Cache.ValueWrapper wrapper = store.get(key);
            if (wrapper != null) {
                Object value = evictHandler.apply(key, wrapper.get());
                if (value == null) {
                    store.remove(key);
                } else {
                    store.put(key, createValueWrapper(value));
                }
            }
            log.debug("evict refresh name:{} key:{}", name, key);
        }
        manager.evictDependents(name, key);
    }

    @Override
    public void clear() {
        doClear();
        manager.clearNotify(name);
    }

    public void onlyClear() {
        doClear();
    }

    private void doClear() {
        store.clear();
        manager.clearDependents(name);
        log.debug("clear name:{}", name);
    }

    private static String toRegexKey(String key) {
        if (key.length() < regexLength) return null;
        char[] cs = key.toCharArray();
        for (int i = 0; i < regexLength; i++) {
            if (regexPrefix[i] != cs[i]) return null;
        }
        return new String(cs, regexLength, cs.length - regexLength);
    }
}
