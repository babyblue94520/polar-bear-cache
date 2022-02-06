package pers.clare.polarbearcache.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.event.EventSender;
import pers.clare.polarbearcache.processor.CacheAliveConfig;
import pers.clare.polarbearcache.processor.CacheAnnotationFactory;
import pers.clare.polarbearcache.support.CacheDependency;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings({"SpringJavaAutowiredMembersInspection", "unused"})
public class BasicCacheManager implements PolarBearCacheManager, CommandLineRunner, DisposableBean {
    private static final Logger log = LogManager.getLogger();

    private final ConcurrentMap<String, BiFunction<String, Object, Object>> evictHandlers = new ConcurrentHashMap<>();

    protected final ConcurrentMap<String, PolarBearCache> cacheMap = new ConcurrentHashMap<>(16);

    @Autowired(required = false)
    private EventSender eventSender;

    @Autowired(required = false)
    private PolarBearCacheDependencies cacheDependencies;

    @Autowired
    private CacheAnnotationFactory cacheAnnotationFactory;

    @Autowired
    private PolarBearCacheProperties properties;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Override
    public void run(String... args) {
        long delay = 60 * 1000;
        executor.scheduleWithFixedDelay(this::expire, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }

    public void expire() {
        long now = System.currentTimeMillis();
        cacheMap.forEach((key, cache) -> cache.expire(now));
    }

    @Override
    public PolarBearCache getCache(String name) {
        return cacheMap.computeIfAbsent(name, createCache);
    }

    public PolarBearCache getCacheOrNull(String name) {
        return cacheMap.get(name);
    }

    protected PolarBearCache createCache(String name) {
        long effectiveTime = properties.getEffectiveTime();
        boolean extension = properties.isExtension();
        CacheAliveConfig config = cacheAnnotationFactory.getCacheAlive(name);
        if (config != null) {
            effectiveTime = config.getEffectiveTime();
            extension = config.isExtension();
        }
        return new BasicCache(this, name, effectiveTime, extension);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheMap.keySet());
    }

    private final Function<? super String, ? extends PolarBearCache> createCache = this::createCache;

    /**
     * Evict cache data and publish evict event
     */
    public void evict(String name, String key) {
        if (name == null || key == null) return;
        PolarBearCache cache = getCacheOrNull(name);
        if (cache == null) {
            evictDependents(name, key);
            evictNotify(name, key);
        } else {
            cache.evict(key);
        }
    }

    /**
     * Evict cache data
     */
    public void onlyEvict(String name, String key) {
        if (name == null || key == null) return;
        PolarBearCache cache = getCacheOrNull(name);
        if (cache == null) {
            evictDependents(name, key);
        } else {
            cache.onlyEvict(key);
        }
        log.debug("evict {} {}", name, key);
    }

    /**
     * Publish event
     */
    public void evictNotify(String name, String key) {
        try {
            if (eventSender == null) return;
            eventSender.send(this, name, key);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Clear cache and publish clear event
     */
    public void clear(String name) {
        if (name == null) return;
        PolarBearCache cache = getCacheOrNull(name);
        if (cache == null) {
            clearDependents(name);
            clearNotify(name);
        } else {
            cache.clear();
        }
    }

    /**
     * Clear cache
     */
    public void onlyClear(String name) {
        if (name == null) return;
        PolarBearCache cache = getCacheOrNull(name);
        if (cache == null) {
            clearDependents(name);
        } else {
            cache.onlyClear();
        }
        log.debug("clear {}", name);
    }

    /**
     * Publish clear event
     */
    @Override
    public void clearNotify(String name) {
        try {
            if (eventSender == null) return;
            eventSender.send(this, name);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Clear all cache data and publish clear all event
     */
    public void clear() {
        onlyClear();
        clearAllNotify();
    }

    /**
     * Clear all cache data
     */
    public void onlyClear() {
        for (PolarBearCache value : cacheMap.values()) {
            value.onlyClear();
        }
    }

    /**
     * Publish clear all event
     */
    public void clearAllNotify() {
        if (eventSender == null) return;
        eventSender.send(this);
    }

    public void evictDependents(String name, String key) {
        if (cacheDependencies == null) return;
        Collection<CacheDependency> dependents = cacheDependencies.find(name);
        if (dependents == null || dependents.size() == 0) return;
        PolarBearCache cache;
        for (CacheDependency dependent : dependents) {
            cache = getCacheOrNull(dependent.getName());
            if (cache == null) continue;
            if (dependent.getAllEntries()) {
                cache.onlyClear();
            } else {
                cache.onlyEvict(dependent.getKeyConverter().apply(key));
            }
        }
    }

    public void clearDependents(String name) {
        if (cacheDependencies == null) return;
        Collection<CacheDependency> dependents = cacheDependencies.find(name);
        if (dependents == null || dependents.size() == 0) return;
        PolarBearCache cache;
        for (CacheDependency dependent : dependents) {
            cache = getCacheOrNull(dependent.getName());
            if (cache == null) continue;
            cache.onlyClear();
        }
    }

    public boolean isMe(CacheManager cacheManager) {
        return this == cacheManager;
    }


    @SuppressWarnings({"unused", "unchecked"})
    public <T> void onEvict(String cacheName, BiFunction<String, T, T> handler) {
        if (evictHandlers.put(cacheName, (BiFunction<String, Object, Object>) handler) != null) {
            log.warn(String.format("%s evict refresh handler duplicate.", cacheName));
        }
    }

    public BiFunction<String, Object, Object> getEvictHandler(String name) {
        return evictHandlers.get(name);
    }
}
