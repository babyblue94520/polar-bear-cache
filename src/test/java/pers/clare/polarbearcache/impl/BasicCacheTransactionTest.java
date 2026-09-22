package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class BasicCacheTransactionTest {

    @AfterEach
    void cleanupSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void putIfAbsentWaitsForCommit() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();

        assertNull(cache.putIfAbsent("key", "value"));
        assertNull(cache.get("key"));

        commit();

        assertEquals("value", cache.get("key").get());
    }

    @Test
    void putNotificationWaitsForCommit() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();

        cache.putNotify("key");

        verify(manager, never()).evictDependents("cache", "key");
        verify(manager, never()).evictNotify("cache", "key");

        commit();

        verify(manager).evictDependents("cache", "key");
        verify(manager).evictNotify("cache", "key");
    }

    @Test
    void putIfAbsentDoesNotCacheWhenEventServiceIsUnavailable() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(false);
        BasicCache cache = new BasicCache(manager, "cache", 0, false);

        assertNull(cache.putIfAbsent("key", "value"));
        assertEquals(0, cache.store.size());
    }

    @Test
    void callableLoadIsKeptAfterCommit() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();

        assertEquals("value", cache.get("key", () -> "value"));

        commit();

        assertEquals("value", cache.get("key").get());
    }

    @Test
    void callableLoadIsKeptAfterRollback() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();

        assertEquals("value", cache.get("key", () -> "value"));

        rollback();

        assertEquals("value", cache.get("key").get());
    }

    private void commit() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        synchronizations.forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void rollback() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        TransactionSynchronizationManager.clearSynchronization();
    }
}
