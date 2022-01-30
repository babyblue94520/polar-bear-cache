package pers.clare.polarbearcache;


import java.util.function.Consumer;

public interface PolarBearCacheEventService {

    Runnable onConnected(Runnable runnable);

    String send(String topic, String body);

    Consumer<String> addListener(String topic, Consumer<String> listener);
}
