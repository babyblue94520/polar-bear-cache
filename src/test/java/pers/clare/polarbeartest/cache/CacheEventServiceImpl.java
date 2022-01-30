package pers.clare.polarbeartest.cache;

import pers.clare.polarbearcache.impl.AbstractCacheEventService;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class CacheEventServiceImpl extends AbstractCacheEventService implements InitializingBean {
    private static final Map<String, List<Consumer<String>>> topicListenerMap = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    public void afterPropertiesSet() throws Exception {
        publishConnectedEvent();
    }

    @Override
    public String send(String topic, String body) {
        executor.submit(()->{
            topicListenerMap.getOrDefault(topic, Collections.emptyList()).forEach(consumer -> consumer.accept(body));
        });
        return body;
    }

    @Override
    public Consumer<String> addListener(String topic, Consumer<String> listener) {
        topicListenerMap.computeIfAbsent(topic, (key) -> new CopyOnWriteArrayList<>()).add(listener);
        return listener;
    }
}
