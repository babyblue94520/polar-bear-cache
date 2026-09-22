package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class CacheRecoveryTest {
    @Test
    void preOutageTransactionCannotRepopulateRecoveredStorage() {
        PolarBearCacheEventService service = mock(PolarBearCacheEventService.class);
        when(service.isAvailable()).thenReturn(true);
        BasicCacheManager manager = new BasicCacheManager(
                new CacheAnnotationFactory(), new PolarBearCacheProperties(), null, service);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            BasicCache cache = (BasicCache) manager.getCache("cache");
            cache.put("put", "stale");
            cache.putIfAbsent("absent", "stale");
            when(service.getInvalidationVersion()).thenReturn(1L);
            assertNull(cache.get("put"));
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            assertNull(cache.get("put"));
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
            BasicCache first = (BasicCache) manager.getCache("first");
            BasicCache unread = (BasicCache) manager.getCache("unread");
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
