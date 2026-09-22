package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheReloadSafetyTest {
    private BasicCacheManager manager() {
        BasicCacheManager manager = mock(BasicCacheManager.class);
        when(manager.isCacheable()).thenReturn(true);
        return manager;
    }

    @Test
    void failedEvictStillInvalidatesAndNotifies() {
        BasicCacheManager manager = manager();
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        cache.put("key", "old");
        when(manager.getEvictHandler("cache")).thenReturn((key, value) -> {
            throw new IllegalStateException("reload failed");
        });
        assertDoesNotThrow(() -> cache.evict("key"));
        assertNull(cache.get("key"));
        verify(manager).evictDependents("cache", "key");
        verify(manager).evictNotify("cache", "key");
    }

    @Test
    void failedClearContinuesThroughAllEntriesAndNotifies() {
        BasicCacheManager manager = manager();
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        cache.put("a", "old");
        cache.put("b", "old");
        when(manager.getEvictHandler("cache")).thenReturn((key, value) -> {
            throw new IllegalStateException("reload failed");
        });
        assertDoesNotThrow(cache::clear);
        assertTrue(cache.store.isEmpty());
        verify(manager).dispatchClear("cache");
        verify(manager).clearDependents("cache");
        verify(manager).clearNotify("cache");
    }

    @Test
    void reloadCannotWriteIntoRecoveredStorage() {
        BasicCacheManager manager = manager();
        BasicCache cache = new BasicCache(manager, "cache", 0, false);
        cache.put("key", "old");
        when(manager.getEvictHandler("cache")).thenReturn((key, value) -> {
            // Recovery happens after the handler reads the old entry.
            cache.discardStorage();
            return "stale reload";
        });
        cache.onlyEvict("key");
        assertNull(cache.get("key"));
    }

    @Test
    void clearPreservesReplacementWrittenWhileHandlerRuns() {
        for (boolean reload : new boolean[]{false, true}) {
            BasicCacheManager manager = manager();
            BasicCache cache = new BasicCache(manager, "cache", 0, false);
            cache.put("key", "old");
            when(manager.getEvictHandler("cache")).thenReturn((key, value) -> {
                // Interleave a write between observing the entry and applying the result.
                cache.put(key, "fresh");
                return reload ? "stale reload" : null;
            });
            cache.onlyClear();
            assertEquals("fresh", cache.get("key").get());
        }
    }
}
