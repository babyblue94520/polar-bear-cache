package pers.clare.polarbearcache;

import org.springframework.cache.CacheManager;

import java.util.function.BiFunction;


@SuppressWarnings("unused")
public interface PolarBearCacheManager extends CacheManager {

    void evict(String name, String key);

    /**
     * Evict cache data
     */
    void onlyEvict(String name, String key);

    void evictNotify(String name, String key);

    /**
     * Clear cache and publish clear event
     */
    void clear(String name);

    /**
     * Clear cache
     */
    void onlyClear(String name);

    /**
     * Clear all cache data and publish clear all event
     */
    void clear();

    /**
     * Clear all cache data
     */
    void onlyClear();

    void clearNotify(String name);

    /**
     * Publish clear all event
     */
    void clearAllNotify();

    void evictDependents(String name, String key);

    void clearDependents(String name);

    boolean isMe(CacheManager cacheManager);

    <T> void onEvict(String cacheName, BiFunction<String, T, T> handler);

    BiFunction<String, Object, Object> getEvictHandler(String name);
}
