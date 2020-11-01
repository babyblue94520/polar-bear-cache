package pers.clare.core.mq;

import java.util.function.Consumer;

public interface MQService {

    public void send(String topic, String body);

    public void listener(String topic, Consumer<String> listener);
}
