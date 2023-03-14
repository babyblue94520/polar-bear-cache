package pers.clare.polarbearcache;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import pers.clare.polarbearcache.aop.CachePutAop;
import pers.clare.polarbearcache.event.EventReceiver;
import pers.clare.polarbearcache.event.EventSender;
import pers.clare.polarbearcache.event.EventSenderQueue;
import pers.clare.polarbearcache.impl.BasicCacheManager;
import pers.clare.polarbearcache.impl.CacheDependenciesImpl;
import pers.clare.polarbearcache.impl.CompositeCacheManagerImpl;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

@ConditionalOnBean(PolarBearCacheConfiguration.class)
@AutoConfigureBefore(CacheAutoConfiguration.class)
public class PolarBearCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventSenderQueue.class)
    public EventSenderQueue<CacheManager> eventSenderQueue() {
        return new EventSenderQueue<>();
    }

    @Bean
    @ConditionalOnMissingBean(CachePutAop.class)
    public CachePutAop cachePutAop(
            CompositePolarBearCacheManager cacheManager
            , CacheAnnotationFactory cacheAnnotationFactory
    ) {
        return new CachePutAop(cacheManager, cacheAnnotationFactory);
    }

    @Bean
    @ConditionalOnMissingBean(EventSender.class)
    public EventSender eventSender(
            @Nullable PolarBearCacheEventService eventService
            , EventSenderQueue<CacheManager> senderQueue
    ) {
        return new EventSender(eventService, senderQueue);
    }


    @Bean
    @ConditionalOnMissingBean(PolarBearCacheDependencies.class)
    public PolarBearCacheDependencies cacheDependencies() {
        return new CacheDependenciesImpl();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(PolarBearCacheManager.class)
    public PolarBearCacheManager cacheManager(
            CacheAnnotationFactory cacheAnnotationFactory
            , PolarBearCacheProperties properties
            , PolarBearCacheDependencies cacheDependencies
            , EventSender eventSender
    ) {
        return new BasicCacheManager(cacheAnnotationFactory, properties, cacheDependencies, eventSender);
    }

    @Bean
    @ConditionalOnMissingBean(CompositePolarBearCacheManager.class)
    public CompositePolarBearCacheManager compositeCacheManager(
            PolarBearCacheManager... polarBearCacheManagers
    ) {
        return new CompositeCacheManagerImpl(polarBearCacheManagers);
    }

    @Bean
    @ConditionalOnBean(CompositePolarBearCacheManager.class)
    public EventReceiver eventReceiver(
            CompositePolarBearCacheManager cacheManager
            , EventSenderQueue<CacheManager> senderQueue
            , @Nullable PolarBearCacheEventService eventService
    ) {
        return new EventReceiver(cacheManager, senderQueue, eventService);
    }

}
