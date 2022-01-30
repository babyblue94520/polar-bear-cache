package pers.clare.polarbearcache.event;

import pers.clare.polarbearcache.support.VolatileSenderQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class EventSenderQueue<T> {

    private final VolatileSenderQueue<T> senderQueue = new VolatileSenderQueue<>(5000);

    private final ConcurrentMap<String, VolatileSenderQueue<T>> clearSenderQueue = new ConcurrentHashMap<>(16);

    private final ConcurrentMap<String, ConcurrentMap<String, VolatileSenderQueue<T>>> evictBlocks = new ConcurrentHashMap<>(16);

    public T poll() {
        return senderQueue.poll();
    }

    public T poll(String name) {
        return getClearBlock(name).poll();
    }

    public T poll(String name, String key) {
        return getEvictBlock(name, key).poll();
    }

    public void add(T t) {
        senderQueue.add(t);
    }

    public void add(T t, String name) {
        getClearBlock(name).add(t);
    }

    public void add(T t, String name, String key) {
        getEvictBlock(name, key).add(t);
    }

    private final Function<? super String, ? extends ConcurrentMap<String, VolatileSenderQueue<T>>> createConcurrentHashMap = (k) -> new ConcurrentHashMap<>();

    private final Function<? super String, ? extends VolatileSenderQueue<T>> createVolatileBlock = (k) -> new VolatileSenderQueue<>(5000);

    private VolatileSenderQueue<T> getClearBlock(String name) {
        return clearSenderQueue.computeIfAbsent(name, createVolatileBlock);
    }

    private VolatileSenderQueue<T> getEvictBlock(String name, String key) {
        return evictBlocks
                .computeIfAbsent(name, createConcurrentHashMap)
                .computeIfAbsent(key, createVolatileBlock);
    }
}
