package pers.clare.polarbearcache.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.event.EventSender;
import pers.clare.polarbearcache.proccessor.CacheAliveConfig;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;
import pers.clare.polarbearcache.support.CacheDependency;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BasicCacheManager implements PolarBearCacheManager, CommandLineRunner, DisposableBean {
    private static final Logger log = LogManager.getLogger();

    private final ConcurrentMap<String, BiFunction<String, Object, Object>> evictHandlers = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, List<Runnable>> clearHandlers = new ConcurrentHashMap<>();

    protected final ConcurrentMap<String, PolarBearCache> cacheMap = new ConcurrentHashMap<>(16);


    private final CacheAnnotationFactory cacheAnnotationFactory;


    private final PolarBearCacheProperties properties;


    private final PolarBearCacheDependencies cacheDependencies;

    private final EventSender eventSender;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public BasicCacheManager(CacheAnnotationFactory cacheAnnotationFactory, PolarBearCacheProperties properties, PolarBearCacheDependencies cacheDependencies, EventSender eventSender) {
        this.cacheAnnotationFactory = cacheAnnotationFactory;
        this.properties = properties;
        this.cacheDependencies = cacheDependencies;
        this.eventSender = eventSender;
    }

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
    public Cache getCache(String name) {
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
        Cache cache = getCacheOrNull(name);
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
            dispatchClear(name);
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
        dispatchClear();
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

    public void onClear(String cacheName, Runnable runnable) {
        clearHandlers.computeIfAbsent(cacheName, (key) -> new CopyOnWriteArrayList<>()).add(runnable);
    }

    void dispatchClear() {
        for (List<Runnable> list : clearHandlers.values()) {
            run(list);
        }
    }

    void dispatchClear(String name) {
        run(clearHandlers.get(name));
    }

    void run(Collection<Runnable> list) {
        if (list == null) return;
        for (Runnable runnable : list) {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    BiFunction<String, Object, Object> getEvictHandler(String name) {
        return evictHandlers.get(name);
    }

    public boolean isCacheable(){
        return eventSender.isAvailable();
    }
}
