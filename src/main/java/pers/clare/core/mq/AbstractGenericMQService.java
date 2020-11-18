package pers.clare.core.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import pers.clare.core.json.JsonUtil;
import pers.clare.core.json.RewriteTypeReference;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @param <T>
 */
@Log4j2
public abstract class AbstractGenericMQService<T> implements GenericMQService<T> {
    protected final String topic;

    protected final TypeReference<T> type;

    protected boolean string;

    private List<GenericReceiveListener<T>> listeners = new ArrayList<>();

    private final boolean registerListener;

    private MQService mqService;

    public static final ExecutorService mqExecutor = Executors.newFixedThreadPool(5
            , new ThreadFactoryBuilder().setNameFormat("mq-%d").build());

    public AbstractGenericMQService(
            String topic
    ) {
        this(null, topic, true);
    }

    public AbstractGenericMQService(
            String topic
            , boolean registerListener
    ) {
        this(null, topic, registerListener);
    }

    public AbstractGenericMQService(
            MQService mqService
            , String topic
    ) {
        this(mqService, topic, true);
    }

    public AbstractGenericMQService(
            MQService mqService
            , String topic
            , boolean registerListener
    ) {
        this.mqService = mqService;
        this.topic = topic;
        this.registerListener = registerListener;

        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        string = String.class.equals(t);
        this.type = string ? null : new RewriteTypeReference<T>(t) {
        };
        registerListener();
    }

    @Autowired
    public void setMQService(MQService mqService) {
        if (this.mqService != null) return;
        this.mqService = mqService;
        this.registerListener();
    }

    private void registerListener() {
        if (mqService == null) return;
        if (registerListener) {
            mqService.listener(this.topic, (body) -> {
                this.receive(body);
            });
        }
    }

    private void receive(String body) {
        if (body == null || listeners.size() == 0) {
            log.trace("body is null or not listener");
            return;
        }
        try {
            T data = string ? (T) body : JsonUtil.decode(body, this.type);
            long time = System.currentTimeMillis();
            log.trace("{} body:{}", time, body);
            for (GenericReceiveListener<T> listener : listeners) {
                mqExecutor.execute(() -> {
                    try {
                        listener.accept(body, data);
                        log.trace("{} {}ms", time, System.currentTimeMillis() - time);
                    } catch (Exception e) {
                        log.error("{} {}", time, e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.error("{} body:{}", e.getMessage(), body);
        }
    }

    public T send(T t) {
        if (mqService == null) return t;
        try {
            mqService.send(this.topic, string ? (String) t : JsonUtil.encode(t));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return t;
    }

    public GenericReceiveListener<T> addListener(GenericReceiveListener<T> listener) {
        if (!registerListener) throw new UnsupportedOperationException();
        this.listeners.add(listener);
        return listener;
    }

}
