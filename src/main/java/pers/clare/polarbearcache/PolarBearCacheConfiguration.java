package pers.clare.polarbearcache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pers.clare.polarbearcache.aop.CachePutAop;
import pers.clare.polarbearcache.event.EventReceiver;
import pers.clare.polarbearcache.event.EventSender;
import pers.clare.polarbearcache.event.EventSenderQueue;
import pers.clare.polarbearcache.processor.CacheAnnotationFactory;

@Import({PolarBearCacheProperties.class, CacheAnnotationFactory.class})
@EnableCaching
@Configuration
public class PolarBearCacheConfiguration {
    private final PolarBearCacheProperties properties;

    public PolarBearCacheConfiguration(PolarBearCacheProperties properties) {
        this.properties = properties;
    }

    @Bean
    public EventSenderQueue<CacheManager> eventSenderQueue() {
        return new EventSenderQueue<>();
    }

    @Bean
    public CachePutAop cachePutAop() {
        return new CachePutAop();
    }

    @Bean
    public EventSender eventSender() {
        return new EventSender(properties.getTopic());
    }

    @Bean
    public EventReceiver eventReceiver() {
        return new EventReceiver(properties.getTopic());
    }

}
