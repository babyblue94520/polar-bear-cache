package pers.clare.polarbearcache;

import org.springframework.cache.CacheManager;

public interface CompositePolarBearCacheManager extends PolarBearCacheManager {
    void onlyClear(CacheManager poll);

    void onlyClear(String name, CacheManager poll);

    void onlyEvict(String name, String key, CacheManager poll);
}
