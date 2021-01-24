package pers.clare.core.mq;

import java.util.function.Consumer;

public interface MQService {

    void send(String topic, String body);

    void listener(String topic, Consumer<String> listener);

    void onConnected(Runnable runnable);
}
