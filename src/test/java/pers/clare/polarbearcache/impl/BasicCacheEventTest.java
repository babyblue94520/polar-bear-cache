package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BasicCacheEventTest {

    @Test
    void ownEventIsIgnoredButExternalEventIsApplied() {
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        when(eventService.isAvailable()).thenReturn(true);
        BasicCacheManager manager = manager(eventService);
        ArgumentCaptor<Consumer<String>> listener = consumerCaptor();
        manager.afterPropertiesSet();
        verify(eventService).addListener(listener.capture());
        manager.getCache("cache").put("key", "value");

        manager.evictNotify("cache", "key");
        listener.getValue().accept("cache\nkey");

        assertNotNull(manager.getCache("cache").get("key"));

        listener.getValue().accept("cache\nkey");

        assertNull(manager.getCache("cache").get("key"));
    }

    @Test
    void failedSendRemovesSelfEventMarker() {
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        when(eventService.isAvailable()).thenReturn(true);
        BasicCacheManager manager = manager(eventService);
        manager.getCache("cache").put("key", "value");
        doThrow(new IllegalStateException("send failed")).when(eventService).send("cache\nkey");

        manager.evictNotify("cache", "key");
        manager.receive("cache\nkey");

        assertNull(manager.getCache("cache").get("key"));
    }

    @Test
    void eventCodecPreservesNewlinesInNameAndKey() {
        BasicCacheManager manager = manager(null);
        String name = "cache\nname";
        String key = "key\nvalue";
        manager.getCache(name).put(key, "value");

        manager.receive(
                pers.clare.polarbearcache.event.EventDataCodec.encode(name)
                        + "\n"
                        + pers.clare.polarbearcache.event.EventDataCodec.encode(key)
        );

        assertNull(manager.getCache(name).get(key));
    }

    @Test
    void noEventServiceKeepsCacheAvailable() {
        assertTrue(manager(null).isCacheable());
    }

    private static BasicCacheManager manager(PolarBearCacheEventService eventService) {
        return new BasicCacheManager(
                mock(CacheAnnotationFactory.class)
                , mock(PolarBearCacheProperties.class)
                , mock(PolarBearCacheDependencies.class)
                , eventService
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Consumer<String>> consumerCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Consumer.class);
    }
}
