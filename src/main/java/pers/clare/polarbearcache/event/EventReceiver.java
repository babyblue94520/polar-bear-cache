package pers.clare.polarbearcache.event;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import pers.clare.polarbearcache.CompositePolarBearCacheManager;
import pers.clare.polarbearcache.PolarBearCacheEventService;

public class EventReceiver implements InitializingBean {
    private static final String split = "\n";

    private final CompositePolarBearCacheManager cacheManager;

    private final EventSenderQueue<CacheManager> senderQueue;

    @Nullable
    private final PolarBearCacheEventService eventService;

    public EventReceiver(CompositePolarBearCacheManager cacheManager, EventSenderQueue<CacheManager> senderQueue, @Nullable PolarBearCacheEventService eventService) {
        this.cacheManager = cacheManager;
        this.senderQueue = senderQueue;
        this.eventService = eventService;
    }


    @Override
    public void afterPropertiesSet() {
        if (eventService == null) return;
        eventService.addListener(this::parse);
    }

    public void parse(String data) {
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
