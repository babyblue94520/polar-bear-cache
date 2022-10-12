package pers.clare.polarbearcache.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;

public class EventSender {
    private static final Logger log = LogManager.getLogger();

    private static final String split = ",";

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
        try {
            if (eventService == null) return;
            senderQueue.add(cacheManager);
            eventService.send(properties.getTopic(), "");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void send(CacheManager cacheManager, String name) {
        try {
            if (eventService == null) return;
            senderQueue.add(cacheManager, name);
            eventService.send(properties.getTopic(), name);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void send(CacheManager cacheManager, String name, String key) {
        try {
            if (eventService == null) return;
            senderQueue.add(cacheManager, name, key);
            eventService.send(properties.getTopic(), name + split + key);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
