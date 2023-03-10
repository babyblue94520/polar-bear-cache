package pers.clare.polarbearcache.event;

import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;

public class EventSender {
    private static final String split = "\n";

    private final PolarBearCacheProperties properties;
    private final EventSenderQueue<CacheManager> senderQueue;
    @Nullable
    private final PolarBearCacheEventService eventService;

    public EventSender(PolarBearCacheProperties properties, @Nullable PolarBearCacheEventService eventService, EventSenderQueue<CacheManager> senderQueue) {
        this.properties = properties;
        this.senderQueue = senderQueue;
        this.eventService = eventService;
    }


    public void send(CacheManager cacheManager) {
        if (eventService == null) return;
        senderQueue.add(cacheManager);
        eventService.send(properties.getTopic(), "");
    }

    public void send(CacheManager cacheManager, String name) {
        if (eventService == null) return;
        senderQueue.add(cacheManager, name);
        eventService.send(properties.getTopic(), name);
    }

    public void send(CacheManager cacheManager, String name, String key) {
        if (eventService == null) return;
        senderQueue.add(cacheManager, name, key);
        eventService.send(properties.getTopic(), name + split + key);
    }

    public boolean isAvailable() {
        if (eventService == null) return true;
        return eventService.isAvailable();
    }
}
