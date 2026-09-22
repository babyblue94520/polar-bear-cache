package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BasicCacheEvictTest {

    @Test
    void evictClearsSourceAndDependentThenPublishesOneEvent() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        BasicCacheManager manager = manager(dependencies, eventService);

        manager.getCache("source").put("key", "source-value");
        manager.getCache("dependent").put("key", "dependent-value");
        dependencies.depend("dependent", "source");

        manager.evict("source", "key");

        assertNull(manager.getCache("source").get("key"));
        assertNull(manager.getCache("dependent").get("key"));
        verify(eventService).send("source\nkey");
    }

    @Test
    void evictPublishesWhenSourceCacheHasNotBeenCreated() {
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        BasicCacheManager manager = manager(mock(PolarBearCacheDependencies.class), eventService);

        manager.evict("source", "key");

        verify(eventService).send("source\nkey");
    }

    @Test
    void onlyEvictClearsWithoutPublishing() {
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        BasicCacheManager manager = manager(mock(PolarBearCacheDependencies.class), eventService);
        manager.getCache("source").put("key", "value");

        manager.onlyEvict("source", "key");

        assertNull(manager.getCache("source").get("key"));
        verify(eventService, never()).send(anyString());
    }

    @Test
    void allEntriesEvictClearsTransitiveDependents() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        BasicCacheManager manager = manager(dependencies, eventService);
        manager.getCache("source");
        manager.getCache("all-entries");
        manager.getCache("leaf");
        dependencies.depend("all-entries", true, "source");
        dependencies.depend("leaf", "all-entries");
        AtomicInteger allEntriesCalls = new AtomicInteger();
        AtomicInteger leafCalls = new AtomicInteger();
        manager.onClear("all-entries", allEntriesCalls::incrementAndGet);
        manager.onClear("leaf", leafCalls::incrementAndGet);

        manager.evict("source", "key");

        assertEquals(1, allEntriesCalls.get());
        assertEquals(1, leafCalls.get());
        verify(eventService).send("source\nkey");
    }

    @Test
    void onlyEvictHandlesCyclicDependenciesOnce() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        BasicCacheManager manager = manager(dependencies, null);
        manager.getCache("first").put("key", "first");
        manager.getCache("second").put("key", "second");
        dependencies.depend("second", "first");
        dependencies.depend("first", "second");
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        manager.onEvict("first", (key, value) -> {
            firstCalls.incrementAndGet();
            return null;
        });
        manager.onEvict("second", (key, value) -> {
            secondCalls.incrementAndGet();
            return null;
        });

        manager.onlyEvict("first", "key");

        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    @Test
    void onlyEvictTraversesMissingIntermediateCache() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        BasicCacheManager manager = manager(dependencies, null);
        manager.getCache("source");
        manager.getCache("leaf").put("key", "leaf");
        dependencies.depend("middle", "source");
        dependencies.depend("leaf", "middle");
        AtomicInteger leafCalls = new AtomicInteger();
        manager.onEvict("leaf", (key, value) -> {
            leafCalls.incrementAndGet();
            return null;
        });

        manager.onlyEvict("source", "key");

        assertEquals(1, leafCalls.get());
        assertNull(manager.getCache("leaf").get("key"));
    }

    @Test
    void allEntriesDiamondClearsLeafOnce() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        BasicCacheManager manager = manager(dependencies, null);
        manager.getCache("source");
        manager.getCache("left");
        manager.getCache("right");
        manager.getCache("leaf");
        dependencies.depend("left", true, "source");
        dependencies.depend("right", true, "source");
        dependencies.depend("leaf", "left", "right");
        AtomicInteger leafCalls = new AtomicInteger();
        manager.onClear("leaf", leafCalls::incrementAndGet);

        manager.onlyEvict("source", "key");

        assertEquals(1, leafCalls.get());
    }

    private BasicCacheManager manager(
            PolarBearCacheDependencies dependencies
            , PolarBearCacheEventService eventService
    ) {
        if (eventService != null) when(eventService.isAvailable()).thenReturn(true);
        return new BasicCacheManager(
                mock(CacheAnnotationFactory.class)
                , mock(PolarBearCacheProperties.class)
                , dependencies
                , eventService
        );
    }
}
