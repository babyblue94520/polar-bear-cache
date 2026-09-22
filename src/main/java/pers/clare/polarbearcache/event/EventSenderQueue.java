package pers.clare.polarbearcache.event;

import pers.clare.polarbearcache.support.VolatileSenderQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class EventSenderQueue {

    private static final long blockEffectiveTime = 5000;
    private static final long cleanupInterval = 5000;

    private final VolatileSenderQueue<Boolean> senderQueue =
            new VolatileSenderQueue<>(blockEffectiveTime);

    private final ConcurrentMap<String, VolatileSenderQueue<Boolean>> clearSenderQueue =
            new ConcurrentHashMap<>(16);

    private final ConcurrentMap<String, ConcurrentMap<String, VolatileSenderQueue<Boolean>>> evictBlocks =
            new ConcurrentHashMap<>(16);

    private final AtomicLong lastCleanupTime = new AtomicLong();

    public boolean poll() {
        cleanupExpiredBlocks();
        return Boolean.TRUE.equals(senderQueue.poll());
    }

    public boolean poll(String name) {
        AtomicBoolean result = new AtomicBoolean();
        clearSenderQueue.compute(name, (key, queue) -> {
            if (queue == null) return null;
            result.set(Boolean.TRUE.equals(queue.poll()));
            return queue.size() == 0 ? null : queue;
        });
        return result.get();
    }

    public boolean poll(String name, String key) {
        AtomicBoolean result = new AtomicBoolean();
        evictBlocks.compute(name, (cacheName, blocks) -> {
            if (blocks == null) return null;
            blocks.compute(key, (cacheKey, queue) -> {
                if (queue == null) return null;
                result.set(Boolean.TRUE.equals(queue.poll()));
                return queue.size() == 0 ? null : queue;
            });
            return blocks.isEmpty() ? null : blocks;
        });
        return result.get();
    }

    public Object add() {
        cleanupExpiredBlocks();
        return senderQueue.add(Boolean.TRUE);
    }

    public Object add(String name) {
        cleanupExpiredBlocks();
        AtomicReference<Object> marker = new AtomicReference<>();
        clearSenderQueue.compute(name, (key, queue) -> {
            if (queue == null) queue = new VolatileSenderQueue<>(blockEffectiveTime);
            marker.set(queue.add(Boolean.TRUE));
            return queue;
        });
        return marker.get();
    }

    public Object add(String name, String key) {
        cleanupExpiredBlocks();
        AtomicReference<Object> marker = new AtomicReference<>();
        evictBlocks.compute(name, (cacheName, blocks) -> {
            if (blocks == null) blocks = new ConcurrentHashMap<>();
            blocks.compute(key, (cacheKey, queue) -> {
                if (queue == null) queue = new VolatileSenderQueue<>(blockEffectiveTime);
                marker.set(queue.add(Boolean.TRUE));
                return queue;
            });
            return blocks;
        });
        return marker.get();
    }

    public void remove(Object marker) {
        senderQueue.remove(marker);
    }

    public void remove(Object marker, String name) {
        clearSenderQueue.computeIfPresent(name, (key, queue) -> {
            queue.remove(marker);
            return queue.size() == 0 ? null : queue;
        });
    }

    public void remove(Object marker, String name, String key) {
        evictBlocks.computeIfPresent(name, (cacheName, blocks) -> {
            blocks.computeIfPresent(key, (cacheKey, queue) -> {
                queue.remove(marker);
                return queue.size() == 0 ? null : queue;
            });
            return blocks.isEmpty() ? null : blocks;
        });
    }

    private void cleanupExpiredBlocks() {
        long now = System.currentTimeMillis();
        long last = lastCleanupTime.get();
        if (now - last < cleanupInterval || !lastCleanupTime.compareAndSet(last, now)) return;

        clearSenderQueue.forEach((name, queue) -> clearSenderQueue.computeIfPresent(name, (key, current) -> {
            if (current != queue) return current;
            return queue.removeExpired() ? null : queue;
        }));

        evictBlocks.forEach((name, blocks) -> evictBlocks.computeIfPresent(name, (cacheName, currentBlocks) -> {
            if (currentBlocks != blocks) return currentBlocks;
            blocks.forEach((key, queue) -> blocks.computeIfPresent(key, (cacheKey, current) -> {
                if (current != queue) return current;
                return queue.removeExpired() ? null : queue;
            }));
            return blocks.isEmpty() ? null : blocks;
        }));
    }
}
