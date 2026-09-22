package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicCacheCallableTest {

    @Test
    void callableLoadHonorsTtl() throws Exception {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        BasicCache cache = new BasicCache(manager, "test", 30, false);
        AtomicInteger loads = new AtomicInteger();
        Callable<Integer> loader = loads::incrementAndGet;

        assertEquals(1, cache.get("key", loader));
        Thread.sleep(50);
        assertEquals(2, cache.get("key", loader));
        assertEquals(2, loads.get());
    }

    @Test
    void callableLoadDoesNotStoreWhenUnavailable() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(false);
        BasicCache cache = new BasicCache(manager, "test", 0, false);
        AtomicInteger loads = new AtomicInteger();
        Callable<Integer> loader = loads::incrementAndGet;

        assertEquals(1, cache.get("key", loader));
        assertEquals(2, cache.get("key", loader));
        assertEquals(2, loads.get());
    }
}
