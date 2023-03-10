package pers.clare.polarbearcache;


import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public interface PolarBearCacheEventService {

    /**
     * Send cache eviction events.
     */
    String send(String topic, String body);

    /**
     * Listen for cache eviction events.
     */
    Consumer<String> addListener(String topic, Consumer<String> listener);

    /**
     * Affects whether the cache manage can be cached.
     */
    boolean isAvailable();
}
