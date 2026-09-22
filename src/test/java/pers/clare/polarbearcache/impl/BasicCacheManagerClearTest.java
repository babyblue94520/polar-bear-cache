package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BasicCacheManagerClearTest {

    @Test
    void clearDispatchesExistingCacheHandlerOnce() {
        BasicCacheManager manager = manager();
        manager.getCache("cache");
        AtomicInteger calls = new AtomicInteger();
        manager.onClear("cache", calls::incrementAndGet);

        manager.clear();

        assertEquals(1, calls.get());
    }

    @Test
    void clearDispatchesHandlerForCacheThatHasNotBeenCreated() {
        BasicCacheManager manager = manager();
        AtomicInteger calls = new AtomicInteger();
        manager.onClear("cache", calls::incrementAndGet);

        manager.clear();

        assertEquals(1, calls.get());
    }

    @Test
    void clearDispatchesDependentHandlerOnce() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        BasicCacheManager manager = manager(dependencies);
        manager.getCache("source");
        manager.getCache("dependent");
        dependencies.depend("dependent", "source");
        AtomicInteger calls = new AtomicInteger();
        manager.onClear("dependent", calls::incrementAndGet);

        manager.clear();

        assertEquals(1, calls.get());
    }

    @Test
    void onlyClearHandlesCyclicDependenciesOnce() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        BasicCacheManager manager = manager(dependencies);
        manager.getCache("first");
        manager.getCache("second");
        dependencies.depend("second", "first");
        dependencies.depend("first", "second");
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        manager.onClear("first", firstCalls::incrementAndGet);
        manager.onClear("second", secondCalls::incrementAndGet);

        manager.onlyClear("first");

        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    @Test
    void onlyClearHandlesDiamondDependenciesOnce() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        BasicCacheManager manager = manager(dependencies);
        manager.getCache("source");
        manager.getCache("left");
        manager.getCache("right");
        manager.getCache("leaf");
        dependencies.depend("left", "source");
        dependencies.depend("right", "source");
        dependencies.depend("leaf", "left", "right");
        AtomicInteger leafCalls = new AtomicInteger();
        manager.onClear("leaf", leafCalls::incrementAndGet);

        manager.onlyClear("source");

        assertEquals(1, leafCalls.get());
    }

    @Test
    void onlyClearTraversesMissingIntermediateCache() {
        CacheDependenciesImpl dependencies = new CacheDependenciesImpl();
        BasicCacheManager manager = manager(dependencies);
        manager.getCache("source");
        manager.getCache("leaf");
        dependencies.depend("middle", "source");
        dependencies.depend("leaf", "middle");
        AtomicInteger leafCalls = new AtomicInteger();
        manager.onClear("leaf", leafCalls::incrementAndGet);

        manager.onlyClear("source");

        assertEquals(1, leafCalls.get());
    }

    private BasicCacheManager manager() {
        return manager(mock(PolarBearCacheDependencies.class));
    }

    private BasicCacheManager manager(PolarBearCacheDependencies dependencies) {
        return new BasicCacheManager(
                mock(CacheAnnotationFactory.class)
                , mock(PolarBearCacheProperties.class)
                , dependencies
                , null
        );
    }
}
