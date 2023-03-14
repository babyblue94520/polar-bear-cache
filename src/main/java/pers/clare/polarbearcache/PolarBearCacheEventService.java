package pers.clare.polarbearcache;


import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public interface PolarBearCacheEventService {

    /**
     * Send cache eviction events.
     */
    void send( String body);

    /**
     * Listen for cache eviction events.
     */
    void addListener( Consumer<String> listener);

    /**
     * Affects whether the cache manage can be cached.
     */
    boolean isAvailable();
}
