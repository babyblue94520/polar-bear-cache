package pers.clare.polarbearcache.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pers.clare.polarbearcache.*;
import pers.clare.polarbearcache.event.EventDataCodec;
import pers.clare.polarbearcache.proccessor.CacheAliveConfig;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;
import pers.clare.polarbearcache.support.CacheDependency;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;


public class BasicCacheManager implements
        PolarBearCacheManager, CommandLineRunner, DisposableBean, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(BasicCacheManager.class);

    private final ConcurrentMap<String, BiFunction<String, Object, Object>> evictHandlers = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, List<Runnable>> clearHandlers = new ConcurrentHashMap<>();

    protected final ConcurrentMap<String, PolarBearCache> cacheMap = new ConcurrentHashMap<>(16);

    private static final String EVENT_PREFIX = "~pbc1~\n";
    private final String senderId = UUID.randomUUID().toString();
    private final ConcurrentMap<String, FailedNotification> failedNotifications = new ConcurrentHashMap<>();

    private static final class FailedNotification {
        final String message;

        FailedNotification(String message) {
            this.message = message;
        }
    }

    private final CacheAnnotationFactory cacheAnnotationFactory;


    private final PolarBearCacheProperties properties;


    private final PolarBearCacheDependencies cacheDependencies;

    @Nullable
    private final PolarBearCacheEventService eventService;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private volatile long invalidationVersion;
    private final AtomicBoolean recoveryActive = new AtomicBoolean();

    public BasicCacheManager(
            CacheAnnotationFactory cacheAnnotationFactory
            , PolarBearCacheProperties properties
            , PolarBearCacheDependencies cacheDependencies
            , @Nullable PolarBearCacheEventService eventService
    ) {
        this.cacheAnnotationFactory = cacheAnnotationFactory;
        this.properties = properties;
        this.cacheDependencies = cacheDependencies;
        this.eventService = eventService;
        this.invalidationVersion = eventService == null ? 0 : eventService.getInvalidationVersion();
    }

    @Override
    public void afterPropertiesSet() {
        if (eventService != null) eventService.addListener(this::receive);
    }

    @Override
    public void run(String... args) {
        long delay = 60000;
        executor.scheduleWithFixedDelay(this::expire, delay, delay, TimeUnit.MILLISECONDS);
        if (eventService != null) {
            long retryDelay = properties.getNotificationRetryInterval().toMillis();
            executor.scheduleWithFixedDelay(this::retryNotifications, retryDelay, retryDelay, TimeUnit.MILLISECONDS);
        }
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
        return new TransactionCache(this, name, effectiveTime, extension);
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
        var cache = getCache(name);
        if (cache != null) {
            cache.evict(key);
        }
    }

    /**
     * Evict cache data
     */
    public void onlyEvict(String name, String key) {
        if (name == null || key == null) return;
        PolarBearCache cache = getCacheOrNull(name);
        if (cache != null) {
            cache.onlyEvict(key);
        }
        evictDependents(name, key);
        log.debug("evict {} {}", name, key);
    }

    /**
     * Publish event
     */
    public void evictNotify(String name, String key) {
        publish(EventDataCodec.encode(name) + "\n" + EventDataCodec.encode(key));
    }

    /**
     * Clear cache and publish clear event
     */
    public void clear(String name) {
        if (name == null) return;
        var cache = getCache(name);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Clear cache
     */
    public void onlyClear(String name) {
        if (name == null) return;
        PolarBearCache cache = getCacheOrNull(name);
        if (cache != null) {
            cache.onlyClear();
        } else {
            dispatchClear(name);
        }
        clearDependents(name);
        log.debug("clear {}", name);
    }

    /**
     * Publish clear event
     */
    @Override
    public void clearNotify(String name) {
        publish(EventDataCodec.encode(name));
    }

    /**
     * Clear all cache data and publish clear all event
     */
    public void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    onlyClear();
                    clearAllNotify();
                }
            });
        } else {
            onlyClear();
            clearAllNotify();
        }
    }

    /**
     * Clear all cache data
     */
    public void onlyClear() {
        for (PolarBearCache value : cacheMap.values()) {
            value.onlyClear();
        }
        clearHandlers.forEach((name, handlers) -> {
            if (!cacheMap.containsKey(name)) run(handlers);
        });
    }

    /**
     * Publish clear all event
     */
    public void clearAllNotify() {
        publish("");
    }

    private void publish(String body) {
        if (eventService == null) return;
        String message = EVENT_PREFIX + senderId + "\n" + body;
        attemptNotification(body, message, failedNotifications.get(body));
    }

    private boolean attemptNotification(String body, String message, FailedNotification previous) {
        if (eventService == null) return true;
        try {
            eventService.send(message);
            // Remove only the record observed before sending, never a newer failure.
            if (previous != null) failedNotifications.remove(body, previous);
            return true;
        } catch (RuntimeException e) {
            // Each failure gets a fresh identity, including failures of the same retry.
            failedNotifications.put(body, new FailedNotification(message));
            log.error("Cache notification failed; retained for retry", e);
            return false;
        }
    }

    void retryNotifications() {
        for (Map.Entry<String, FailedNotification> entry : failedNotifications.entrySet()) {
            String body = entry.getKey();
            FailedNotification pending = entry.getValue();
            if (failedNotifications.get(body) == pending) {
                if (!attemptNotification(body, pending.message, pending)) return;
            }
        }
    }

    public void evictDependents(String name, String key) {
        if (name == null || key == null) return;
        DependencyTraversal traversal = new DependencyTraversal();
        traversal.activeNames.add(name);
        traversal.clearedCaches.add(name);
        traversal.markEvicted(name, key);
        evictDependents(name, key, traversal);
    }

    private void evictDependents(String name, String key, DependencyTraversal traversal) {
        if (cacheDependencies == null) return;
        Collection<CacheDependency> dependents = cacheDependencies.find(name);
        if (dependents == null || dependents.isEmpty()) return;
        for (CacheDependency dependent : dependents) {
            String dependentName = dependent.getName();
            if (dependentName == null || traversal.activeNames.contains(dependentName)) continue;

            if (Boolean.TRUE.equals(dependent.getAllEntries())) {
                if (!traversal.clearedCaches.add(dependentName)) continue;
                PolarBearCache cache = getCacheOrNull(dependentName);
                if (cache != null) cache.onlyClear();
                clearDependents(dependentName, traversal.clearedCaches);
            } else {
                try {
                    String dependentKey = dependent.getKeyConverter().apply(key);
                    if (dependentKey == null
                        || traversal.clearedCaches.contains(dependentName)
                        || !traversal.markEvicted(dependentName, dependentKey)
                    ) continue;

                    PolarBearCache cache = getCacheOrNull(dependentName);
                    if (cache != null) cache.onlyEvict(dependentKey);

                    traversal.activeNames.add(dependentName);
                    try {
                        evictDependents(dependentName, dependentKey, traversal);
                    } finally {
                        traversal.activeNames.remove(dependentName);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void clearDependents(String name) {
        if (name == null) return;
        Set<String> visited = new HashSet<>();
        visited.add(name);
        clearDependents(name, visited);
    }

    private void clearDependents(String name, Set<String> visited) {
        if (cacheDependencies == null) return;
        Collection<CacheDependency> dependents = cacheDependencies.find(name);
        if (dependents == null || dependents.isEmpty()) return;
        PolarBearCache cache;
        for (CacheDependency dependent : dependents) {
            String dependentName = dependent.getName();
            if (!visited.add(dependentName)) continue;
            cache = getCacheOrNull(dependentName);
            if (cache != null) cache.onlyClear();
            clearDependents(dependentName, visited);
        }
    }

    private static class DependencyTraversal {
        private final Set<String> activeNames = new HashSet<>();
        private final Set<String> clearedCaches = new HashSet<>();
        private final Map<String, Set<String>> evictedKeys = new HashMap<>();

        private boolean markEvicted(String name, String key) {
            return evictedKeys.computeIfAbsent(name, ignored -> new HashSet<>()).add(key);
        }
    }

    @SuppressWarnings({"unused", "unchecked"})
    public <T> void onEvict(String cacheName, BiFunction<String, T, T> handler) {
        if (evictHandlers.put(cacheName, (BiFunction<String, Object, Object>) handler) != null) {
            log.warn("{} evict refresh handler duplicate.", cacheName);
        }
    }

    public void onClear(String cacheName, Runnable runnable) {
        clearHandlers.computeIfAbsent(cacheName, key -> new CopyOnWriteArrayList<>()).add(runnable);
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
                log.error(e.getMessage(), e);
            }
        }
    }

    BiFunction<String, Object, Object> getEvictHandler(String name) {
        return evictHandlers.get(name);
    }

    public boolean isCacheable() {
        if (eventService == null) return true;
        if (!eventService.isAvailable()) return false;
        long version = eventService.getInvalidationVersion();
        if (version == invalidationVersion) return true;
        return recoverCache();
    }

    private boolean recoverCache() {
        if (eventService == null) return true;
        if (!recoveryActive.compareAndSet(false, true)) return false;
        try {
            if (!eventService.isAvailable()) return false;
            long version = eventService.getInvalidationVersion();
            if (version == invalidationVersion) return true;
            // Discard stale data directly: eviction handlers can repopulate it.
            for (PolarBearCache cache : cacheMap.values()) {
                if (cache instanceof BasicCache) ((BasicCache) cache).discardStorage();
                else cache.onlyClear();
            }
            // The event service increments its version on every possible outage.
            if (!eventService.isAvailable() || eventService.getInvalidationVersion() != version) return false;
            invalidationVersion = version;
            return true;
        } finally {
            recoveryActive.set(false);
        }
    }

    void receive(String data) {
        if (data != null && data.startsWith(EVENT_PREFIX)) {
            int end = data.indexOf('\n', EVENT_PREFIX.length());
            if (end < 0) return;
            String origin = data.substring(EVENT_PREFIX.length(), end);
            try {
                if (!UUID.fromString(origin).toString().equals(origin)) return;
            } catch (IllegalArgumentException e) {
                return;
            }
            if (senderId.equals(origin)) return;
            data = data.substring(end + 1);
        }
        // Legacy notifications have no origin and are always treated as remote.
        if (data == null || data.isEmpty()) {
            onlyClear();
            return;
        }

        int separator = data.indexOf('\n');
        if (separator < 0) {
            String name = EventDataCodec.decode(data);
            onlyClear(name);
            return;
        }

        String name = EventDataCodec.decode(data.substring(0, separator));
        String key = EventDataCodec.decode(data.substring(separator + 1));
        onlyEvict(name, key);
    }
}
