package pers.clare.polarbearcache.support;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VolatileSenderQueue<T> {
    private Queue<VolatileSender<T>> queue = new ConcurrentLinkedQueue<>();
    private final long effectiveTime;

    public VolatileSenderQueue(long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public void add(T t) {
        queue.add(new VolatileSender<>(t, System.currentTimeMillis() + effectiveTime));
    }

    public T poll() {
        VolatileSender<T> sender;
        long now = System.currentTimeMillis();
        while ((sender = queue.poll()) != null) {
            if (sender.getValidTime() > now) {
                return sender.getSender();
            }
        }
        return null;
    }

    public int size() {
        return queue.size();
    }
}
