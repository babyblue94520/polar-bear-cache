package pers.clare.polarbeartest.cache;

import pers.clare.polarbearcache.PolarBearCacheEventService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class CacheEventServiceImpl implements PolarBearCacheEventService {

    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    private volatile boolean available = true;
    private final java.util.concurrent.atomic.AtomicLong invalidationVersion = new java.util.concurrent.atomic.AtomicLong();

    @Override
    public void send( String body) {
        executor.submit(() -> listeners.forEach(consumer -> consumer.accept(body)));
    }

    public static void awaitNotifications() {
        try {
            // A barrier on the single worker waits for all previously queued events.
            executor.submit(() -> {}).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for cache notifications", e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new AssertionError("Cache notifications did not finish", e);
        }
    }

    @Override
    public void addListener( Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available){
        if (!available) invalidationVersion.incrementAndGet();
        this.available = available;
    }

    @Override
    public long getInvalidationVersion() {
        return invalidationVersion.get();
    }

}
