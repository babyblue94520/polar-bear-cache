package pers.clare.core.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import pers.clare.core.json.JsonUtil;
import pers.clare.core.json.RewriteTypeReference;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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


    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    public AbstractGenericMQService(String topic) {
        this(topic, true);
    }

    public AbstractGenericMQService(String topic, boolean registerListener) {
        this.topic = topic;
        this.registerListener = registerListener;
        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        string = String.class.equals(t);
        this.type = string ? null : new RewriteTypeReference<T>(t) {
        };
    }


    @Autowired(required = false)
    public void init(MQService mqService) {
        this.mqService = mqService;
        if (mqService == null) return;
        if (registerListener) {
            mqService.listener(this.topic, (body) -> {
                this.receive(body);
            });
        }
    }

    private void receive(String body) {
        if (body == null || listeners.size() == 0) return;
        try {
            T data = string ? (T) body : JsonUtil.decode(body, this.type);
            for (GenericReceiveListener<T> listener : listeners) {
                taskExecutor.execute(() -> {
                    try {
                        log.trace(body);
                        listener.accept(body, data);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
