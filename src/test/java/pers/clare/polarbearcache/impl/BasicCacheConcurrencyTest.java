package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicCacheConcurrencyTest {

    @Test
    void expiredReadDoesNotRemoveConcurrentReplacement() throws Exception {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        BasicCache cache = new BasicCache(manager, "cache", 60_000, false);
        CountDownLatch expiryRead = new CountDownLatch(1);
        CountDownLatch replacementStarted = new CountDownLatch(1);
        cache.store.put("key", new BasicCacheValueWrapper("expired", 0) {
            @Override
            public long getValidTime() {
                expiryRead.countDown();
                try {
                    assertTrue(replacementStarted.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                return 0;
            }
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Cache.ValueWrapper> read = executor.submit(() -> cache.get("key"));
            assertTrue(expiryRead.await(5, TimeUnit.SECONDS));
            Future<?> write = executor.submit(() -> {
                replacementStarted.countDown();
                cache.put("key", "fresh");
            });

            assertNull(read.get(5, TimeUnit.SECONDS));
            write.get(5, TimeUnit.SECONDS);
            assertEquals("fresh", cache.get("key").get());
        } finally {
            executor.shutdownNow();
        }
    }
}
