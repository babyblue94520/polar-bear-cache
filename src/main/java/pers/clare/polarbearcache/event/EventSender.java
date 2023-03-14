package pers.clare.polarbearcache.event;

import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import pers.clare.polarbearcache.PolarBearCacheEventService;

public class EventSender {
    private static final String split = "\n";

    private final EventSenderQueue<CacheManager> senderQueue;
    @Nullable
    private final PolarBearCacheEventService eventService;

    public EventSender(@Nullable PolarBearCacheEventService eventService, EventSenderQueue<CacheManager> senderQueue) {
        this.senderQueue = senderQueue;
        this.eventService = eventService;
    }


    public void send(CacheManager cacheManager) {
        if (eventService == null) return;
        senderQueue.add(cacheManager);
        eventService.send("");
    }

    public void send(CacheManager cacheManager, String name) {
        if (eventService == null) return;
        senderQueue.add(cacheManager, name);
        eventService.send(name);
    }

    public void send(CacheManager cacheManager, String name, String key) {
        if (eventService == null) return;
        senderQueue.add(cacheManager, name, key);
        eventService.send(name + split + key);
    }

    public boolean isAvailable() {
        if (eventService == null) return true;
        return eventService.isAvailable();
    }
}
