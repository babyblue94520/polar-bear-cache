package pers.clare.redis;

import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Service;
import pers.clare.core.mq.MQService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Log4j2
@Service(RedisMQService.NAME)
public class RedisMQService implements MQService, InitializingBean {
    public static final String NAME = "redisMQService";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MyRedisMessageListenerContainer myRedisMessageListenerContainer;

    @Autowired
    private DefaultClientResources defaultClientResources;

    private List<Runnable> connectedListener = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        defaultClientResources.eventBus().get().subscribe((event)->{
            if(event instanceof ConnectedEvent){
                for (Runnable runnable : connectedListener) {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        });
    }

    @Override
    public void onConnected(Runnable runnable) {
        connectedListener.add(runnable);
    }

    @Override
    public void send(String topic, String body) {
        stringRedisTemplate.convertAndSend(topic, body);
    }

    @Override
    public void listener(String topic, Consumer<String> listener) {
        myRedisMessageListenerContainer.addMessageListener((message, pattern) -> {
            log.info("pattern:{},message:{}", new String(pattern), message);
            listener.accept(new String(message.getBody()));
        }, new PatternTopic(topic));
    }

}
