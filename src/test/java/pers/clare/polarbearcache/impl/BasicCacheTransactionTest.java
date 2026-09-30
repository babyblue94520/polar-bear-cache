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
    void putIfAbsentIsImmediateAndSurvivesRollback() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();

        assertNull(cache.putIfAbsent("key", "value"));
        assertEquals("value", cache.get("key").get());

        rollback();

        assertEquals("value", cache.get("key").get());
    }

    @Test
    void putNotificationWaitsForCommit() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);
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
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);

        assertNull(cache.putIfAbsent("key", "value"));
        assertEquals(0, cache.store.size());
    }

    @Test
    void callableLoadIsKeptAfterCommit() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();

        assertEquals("value", cache.get("key", () -> "value"));

        commit();

        assertEquals("value", cache.get("key").get());
    }

    @Test
    void callableLoadIsKeptAfterRollback() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();

        assertEquals("value", cache.get("key", () -> "value"));

        rollback();

        assertEquals("value", cache.get("key").get());
    }

    @Test
    void mutationsWaitForCommit() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);
        cache.put("old", "value");
        TransactionSynchronizationManager.initSynchronization();

        cache.evict("old");
        cache.put("new", "value");
        assertEquals("value", cache.getValue("old"));
        assertNull(cache.get("new"));
        verify(manager, never()).evictNotify("cache", "old");
        commit();

        assertNull(cache.get("old"));
        assertEquals("value", cache.getValue("new"));
        verify(manager).evictNotify("cache", "old");

        TransactionSynchronizationManager.initSynchronization();
        cache.clear();
        assertEquals("value", cache.getValue("new"));
        commit();
        assertNull(cache.get("new"));
        verify(manager).clearNotify("cache");
    }

    @Test
    void rollbackDiscardsMutationsAndNotifications() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);
        cache.put("old", "value");
        TransactionSynchronizationManager.initSynchronization();
        cache.put("new", "value");
        cache.evict("old");
        cache.clear();
        cache.putNotify("new");
        rollback();

        assertEquals("value", cache.getValue("old"));
        assertNull(cache.get("new"));
        verify(manager, never()).evictNotify(anyString(), anyString());
        verify(manager, never()).clearNotify(anyString());
    }

    @Test
    void inheritedEvictionMethodsWaitForCommit() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        TransactionCache cache = new TransactionCache(manager, "cache", 0, false);
        cache.put("first", "value");
        cache.put("second", "value");
        TransactionSynchronizationManager.initSynchronization();

        cache.evictIfPresent("first");
        assertEquals("value", cache.getValue("first"));
        commit();
        assertNull(cache.get("first"));
        assertEquals("value", cache.getValue("second"));

        TransactionSynchronizationManager.initSynchronization();
        cache.invalidate();
        assertEquals("value", cache.getValue("second"));
        commit();
        assertNull(cache.get("second"));
    }

    @Test
    void basicCacheIsImmediateEvenWithSynchronization() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        TransactionSynchronizationManager.initSynchronization();
        cache.put("key", "value");
        assertEquals("value", cache.getValue("key"));
        cache.evict("key");
        assertNull(cache.get("key"));
        cache.put("key", "value");
        cache.clear();
        assertNull(cache.get("key"));
        assertEquals(0, TransactionSynchronizationManager.getSynchronizations().size());
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
