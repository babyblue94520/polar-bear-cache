package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheRecoveryTest {
    @Test
    void concurrentRequestsBypassCacheUntilRecoveryFinishes() throws Exception {
        verifyConcurrentRecovery(false);
    }

    @Test
    void outageDuringRecoveryRequiresAnotherRecovery() throws Exception {
        verifyConcurrentRecovery(true);
    }

    private void verifyConcurrentRecovery(boolean interruptRecovery) throws Exception {
        PolarBearCacheEventService service = mock(PolarBearCacheEventService.class);
        when(service.isAvailable()).thenReturn(true);
        BasicCacheManager manager = new BasicCacheManager(
                new CacheAnnotationFactory(), new PolarBearCacheProperties(), null, service);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        BasicCache cache = new BasicCache(manager, "cache", 0, false) {
            @Override
            void discardStorage() {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Recovery blocked");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                super.discardStorage();
            }
        };
        manager.cacheMap.put("cache", cache);
        try {
            cache.put("key", "old");
            when(service.getInvalidationVersion()).thenReturn(1L);
            Future<Boolean> recovery = executor.submit(manager::isCacheable);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(executor.submit(manager::isCacheable).get(2, TimeUnit.SECONDS));
            if (interruptRecovery) {
                when(service.isAvailable()).thenReturn(false);
                when(service.getInvalidationVersion()).thenReturn(2L);
                assertFalse(manager.isCacheable());
                when(service.isAvailable()).thenReturn(true);
                assertFalse(executor.submit(manager::isCacheable).get(2, TimeUnit.SECONDS));
            }
            release.countDown();
            assertEquals(!interruptRecovery, recovery.get(5, TimeUnit.SECONDS));
            assertTrue(manager.isCacheable());
            assertNull(cache.get("key"));
        } finally {
            release.countDown();
            executor.shutdownNow();
            manager.destroy();
        }
    }

    @Test
    void transactionWritesToCurrentStorageAfterRecovery() {
        PolarBearCacheEventService service = mock(PolarBearCacheEventService.class);
        when(service.isAvailable()).thenReturn(true);
        BasicCacheManager manager = new BasicCacheManager(
                new CacheAnnotationFactory(), new PolarBearCacheProperties(), null, service);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            PolarBearCache cache = (PolarBearCache) manager.getCache("cache");
            cache.put("put", "committed");
            cache.putIfAbsent("absent", "stale");
            when(service.getInvalidationVersion()).thenReturn(1L);
            assertNull(cache.get("put"));
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            assertEquals("committed", cache.getValue("put"));
            assertNull(cache.get("absent"));
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            manager.destroy();
        }
    }

    @Test
    void recoveryDiscardsUnreadEntriesWithoutPublishingOrReloading() {
        PolarBearCacheEventService service = mock(PolarBearCacheEventService.class);
        when(service.isAvailable()).thenReturn(true);
        BasicCacheManager manager = new BasicCacheManager(
                new CacheAnnotationFactory(), new PolarBearCacheProperties(), null, service);
        try {
            PolarBearCache first = (PolarBearCache) manager.getCache("first");
            PolarBearCache unread = (PolarBearCache) manager.getCache("unread");
            first.put("key", "old");
            unread.put("key", "old");
            manager.onEvict("unread", (key, value) -> "reloaded");
            // The entire outage occurs without any cache request.
            when(service.getInvalidationVersion()).thenReturn(1L);
            assertNull(first.get("key"));
            assertNull(unread.get("key"));
            verify(service, never()).send(anyString());
        } finally {
            manager.destroy();
        }
    }
}
