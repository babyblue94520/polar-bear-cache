package pers.clare.polarbearcache.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import pers.clare.polarbearcache.PolarBearCacheEventService;

public class EventSender {
    private static final Logger log = LogManager.getLogger();

    private static final String split = ",";

    private final String topic;

    @Autowired
    private EventSenderQueue<CacheManager> senderQueue;

    @Autowired(required = false)
    private PolarBearCacheEventService eventService;

    public EventSender(String topic) {
        this.topic = topic;
    }

    public void send(CacheManager cacheManager) {
        try {
            if (eventService == null) return;
            senderQueue.add(cacheManager);
            eventService.send(topic, "");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void send(CacheManager cacheManager, String name) {
        try {
            if (eventService == null) return;
            senderQueue.add(cacheManager, name);
            eventService.send(topic, name);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void send(CacheManager cacheManager, String name, String key) {
        try {
            if (eventService == null) return;
            senderQueue.add(cacheManager, name, key);
            eventService.send(topic, name + split + key);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
