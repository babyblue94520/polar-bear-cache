package pers.clare.polarbearcache.support;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
@SuppressWarnings("unused")
public class VolatileSenderQueue<T> {
    private final Queue<VolatileSender<T>> queue = new ConcurrentLinkedQueue<>();
    private final long effectiveTime;

    public VolatileSenderQueue(long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Object add(T t) {
        VolatileSender<T> sender = new VolatileSender<>(t, System.currentTimeMillis() + effectiveTime);
        queue.add(sender);
        return sender;
    }

    public boolean remove(Object marker) {
        return marker != null && queue.remove(marker);
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

    public boolean removeExpired() {
        VolatileSender<T> sender;
        long now = System.currentTimeMillis();
        while ((sender = queue.peek()) != null && sender.getValidTime() <= now) {
            queue.poll();
        }
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
