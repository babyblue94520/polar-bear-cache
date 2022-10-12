package pers.clare.polarbearcache.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import pers.clare.polarbearcache.CompositePolarBearCacheManager;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheProperties;

public class EventReceiver implements InitializingBean {
    private static final Logger log = LogManager.getLogger();

    private static final String split = ",";

    private final PolarBearCacheProperties properties;

    private final CompositePolarBearCacheManager cacheManager;

    private final EventSenderQueue<CacheManager> senderQueue;

    @Nullable
    private final PolarBearCacheEventService eventService;

    public EventReceiver(PolarBearCacheProperties properties, CompositePolarBearCacheManager cacheManager, EventSenderQueue<CacheManager> senderQueue, @Nullable PolarBearCacheEventService eventService) {
        this.properties = properties;
        this.cacheManager = cacheManager;
        this.senderQueue = senderQueue;
        this.eventService = eventService;
    }


    @Override
    public void afterPropertiesSet() {
        if (eventService == null) return;
        eventService.onConnected(this::onlyClear);
        eventService.addListener(properties.getTopic(), this::parse);
    }

    public void parse(String data) {
        log.debug(data);
        String[] array = data.split(split, -1);
        String name, key;
        switch (array.length) {
            case 0:
                onlyClear();
                break;
            case 1:
                name = array[0];
                onlyClear(name);
                break;
            case 2:
                name = array[0];
                key = array[1];
                onlyEvict(name, key);
                break;
        }
    }

    private void onlyClear() {
        cacheManager.onlyClear(senderQueue.poll());
    }

    private void onlyClear(String name) {
        cacheManager.onlyClear(name, senderQueue.poll(name));
    }

    private void onlyEvict(String name, String key) {
        cacheManager.onlyEvict(name, key, senderQueue.poll(name, key));
    }
}
