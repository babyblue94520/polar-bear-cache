package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BasicCacheEventTest {

    @ParameterizedTest
    @ValueSource(strings = {"evict", "clear", "all"})
    void ownEchoIsAlwaysIgnoredAndOtherManagerWithSameNotificationIsApplied(String operation) {
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        when(eventService.isAvailable()).thenReturn(true);
        BasicCacheManager manager = manager(eventService);
        ArgumentCaptor<Consumer<String>> listener = consumerCaptor();
        manager.afterPropertiesSet();
        verify(eventService).addListener(listener.capture());
        manager.getCache("cache").put("key", "value");

        notify(manager, operation);
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(eventService).send(messages.capture());
        String ownMessage = messages.getValue();
        String[] parts = ownMessage.split("\n", 3);
        assertEquals("~pbc1~", parts[0]);
        assertEquals(4, java.util.UUID.fromString(parts[1]).version());
        listener.getValue().accept(ownMessage);
        listener.getValue().accept(ownMessage);

        assertNotNull(manager.getCache("cache").get("key"));

        BasicCacheManager other = manager(eventService);
        notify(other, operation);
        verify(eventService, times(2)).send(messages.capture());
        String otherMessage = messages.getValue();
        assertNotEquals(ownMessage, otherMessage);
        listener.getValue().accept(otherMessage);

        assertNull(manager.getCache("cache").get("key"));
    }

    private static void notify(BasicCacheManager manager, String operation) {
        if (operation.equals("evict")) manager.evictNotify("cache", "key");
        else if (operation.equals("clear")) manager.clearNotify("cache");
        else manager.clearAllNotify();
    }

    @Test
    void failedSendDoesNotSuppressRemoteEvent() {
        PolarBearCacheEventService eventService = mock(PolarBearCacheEventService.class);
        when(eventService.isAvailable()).thenReturn(true);
        BasicCacheManager manager = manager(eventService);
        manager.getCache("cache").put("key", "value");
        doThrow(new IllegalStateException("send failed")).when(eventService).send(anyString());

        manager.evictNotify("cache", "key");
        manager.receive("cache\nkey");

        assertNull(manager.getCache("cache").get("key"));
    }

    @Test
    void eventCodecPreservesNewlinesInNameAndKey() {
        PolarBearCacheEventService events = mock(PolarBearCacheEventService.class);
        BasicCacheManager sender = manager(events);
        BasicCacheManager manager = manager(null);
        String name = "cache\nname";
        String key = "key\nvalue";
        manager.getCache(name).put(key, "value");

        sender.evictNotify(name, key);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(events).send(message.capture());
        manager.receive(message.getValue());

        assertNull(manager.getCache(name).get(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cache\nkey", "cache", ""})
    void acceptsLegacyNotificationsAsRemote(String message) {
        BasicCacheManager manager = manager(null);
        manager.getCache("cache").put("key", "value");
        manager.receive(message);
        assertNull(manager.getCache("cache").get("key"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"~pbc1~\n", "~pbc1~\ninvalid\n", "~pbc1~\n\n"})
    void malformedEnvelopeDoesNotClearCache(String message) {
        BasicCacheManager manager = manager(null);
        manager.getCache("cache").put("key", "value");
        manager.receive(message);
        assertNotNull(manager.getCache("cache").get("key"));
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
