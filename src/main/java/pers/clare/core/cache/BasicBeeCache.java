package pers.clare.core.cache;

import lombok.extern.log4j.Log4j2;
import org.springframework.cache.Cache;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Log4j2
public class BasicBeeCache implements BeeCache {
    protected final ConcurrentMap<Object, Cache.ValueWrapper> store = new ConcurrentHashMap<>();
    protected final BeeCacheManager manager;
    protected final String name;

    protected BasicBeeCache(
            BeeCacheManager manager
            , String name
    ) {
        this.name = name;
        this.manager = manager;
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
    public Cache.ValueWrapper get(Object key) {
        return store.get(String.valueOf(key));
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return (T) getValue(String.valueOf(key));
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return (T) getValue(String.valueOf(key));
    }

    @Override
    public void put(Object key, Object value) {
        store.put(String.valueOf(key), createValueWrapper(value));
    }

    @Override
    public Cache.ValueWrapper putIfAbsent(Object key, Object value) {
        return store.putIfAbsent(String.valueOf(key), createValueWrapper(value));
    }

    public Object getValue(String key) {
        Cache.ValueWrapper value = get(key);
        if (value == null) return null;
        return value.get();
    }

    protected Cache.ValueWrapper createValueWrapper(Object value) {
        return new BeeCacheValueWrapper(value);
    }

    /**
     * 清除資料並發出通知
     *
     * @param key
     */
    @Override
    public void evict(Object key) {
        String str = String.valueOf(key);
        doEvict(str);
        evictNotify(str);
    }

    /**
     * 只清除資料
     *
     * @param key
     */
    public void onlyEvict(String key) {
        if (key == null) return;
        doEvict(key);
    }

    public void evictNotify(String key) {
        try {
            manager.clearNotify(name, key);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void doEvict(String key) {
        if (key.length() > 6) {
            char[] cs = key.toCharArray();
            // 檢查開頭是否為 "regex:"
            if (cs[0] == 'r' && cs[1] == 'e' && cs[2] == 'g' && cs[3] == 'e' && cs[4] == 'x' && cs[5] == ':') {
                Pattern pattern = Pattern.compile(new String(cs, 6, cs.length - 6));
                for (Object k : store.keySet()) {
                    if (pattern.matcher(k.toString()).find()) {
                        store.remove(k);
                        manager.clearDependents(name, key);
                        log.debug("evict name:{} key:{}", name, k);
                    }
                }
                return;
            }
        }
        store.remove(key);
        manager.clearDependents(name, key);
        log.debug("evict name:{} key:{}", name, key);
    }

    /**
     * 清除資料並發出通知
     */
    @Override
    public void clear() {
        doClear();
        // 不重複清除自己發出的通知
        try {
            manager.clearNotify(name);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 只清除資料
     */
    public void onlyClear() {
        // 不重複清除自己發出的通知
        doClear();
    }

    private void doClear() {
        store.clear();
        manager.clearDependents(name);
        log.debug("clear name:{}", name);
    }
}
