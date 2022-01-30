package pers.clare.polarbearcache.impl;

import pers.clare.polarbearcache.CompositePolarBearCacheManager;
import pers.clare.polarbearcache.PolarBearCacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

public class CompositeCacheManagerImpl implements CompositePolarBearCacheManager {

    private final PolarBearCacheManager[] cacheManagers;

    public CompositeCacheManagerImpl(@NonNull PolarBearCacheManager[] cacheManagers) {
        this.cacheManagers = cacheManagers;
    }

    @Override
    public Cache getCache(String name) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) return cache;
        }
        return null;
    }

    @Override
    public Collection<String> getCacheNames() {
        List<String> names = new ArrayList<>();
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            names.addAll(cacheManager.getCacheNames());
        }
        return names;
    }

    @Override
    public void evict(String name, String key) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.evict(name, key);
        }
    }

    @Override
    public void onlyEvict(String name, String key) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.onlyEvict(name, key);
        }
    }

    @Override
    public void evictNotify(String name, String key) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.evictNotify(name, key);
        }
    }

    @Override
    public void clear(String name) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.clear(name);
        }
    }

    @Override
    public void onlyClear(String name) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.onlyClear(name);
        }
    }

    @Override
    public void clearNotify(String name) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.clearNotify(name);
        }
    }

    @Override
    public void clear() {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.clear();
        }
    }

    @Override
    public void onlyClear() {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.onlyClear();
        }
    }

    @Override
    public void clearAllNotify() {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.clearAllNotify();
        }
    }

    @Override
    public void evictDependents(String name, String key) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.evictDependents(name, key);
        }
    }

    @Override
    public void clearDependents(String name) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            cacheManager.clearDependents(name);
        }
    }

    @Override
    public boolean isMe(CacheManager cacheManager) {
        return false;
    }

    public void onlyClear(CacheManager ignore) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            if (cacheManager.isMe(ignore)) continue;
            cacheManager.onlyClear();
        }
    }

    public void onlyClear(String name, @Nullable CacheManager ignore) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            if (cacheManager.isMe(ignore)) continue;
            cacheManager.onlyClear(name);
        }
    }

    public void onlyEvict(String name, String key, @Nullable CacheManager ignore) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            if (cacheManager.isMe(ignore)) continue;
            cacheManager.onlyEvict(name, key);
        }
    }

    @SuppressWarnings({"unused"})
    public <T> void onEvict(String cacheName, BiFunction<String, T, T> handler) {
        throw new UnsupportedOperationException(String.format("%s does not support this onEvict.", CompositeCacheManagerImpl.class));
    }

    public BiFunction<String, Object, Object> getEvictHandler(String name) {
        throw new UnsupportedOperationException(String.format("%s does not support this onEvict.", CompositeCacheManagerImpl.class));
    }
}
