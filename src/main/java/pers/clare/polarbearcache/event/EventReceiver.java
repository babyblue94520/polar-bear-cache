package pers.clare.polarbearcache.event;

import pers.clare.polarbearcache.CompositePolarBearCacheManager;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

public class EventReceiver implements InitializingBean {
    private static final Logger log = LogManager.getLogger();

    private static final String split = ",";

    private final String topic;

    @Autowired
    private EventSenderQueue<CacheManager> senderQueue;

    @Autowired(required = false)
    private PolarBearCacheEventService eventService;

    @Autowired
    private CompositePolarBearCacheManager cacheManager;

    public EventReceiver(String topic) {
        this.topic = topic;
    }

    @Override
    public void afterPropertiesSet() {
        if (eventService == null) return;
        eventService.onConnected(this::onlyClear);
        eventService.addListener(topic, this::parse);
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
