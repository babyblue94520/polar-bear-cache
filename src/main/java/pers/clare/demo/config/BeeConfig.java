package pers.clare.demo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pers.clare.core.cache.BeeCacheManager;
import pers.clare.core.cache.BusyBeeCacheManager;
import pers.clare.core.mq.MQService;
import pers.clare.redis.RedisMQService;

@Configuration
public class BeeConfig {
    private static final String PT60S = "PT60S";


    // 闲置 60 秒清除 Cache
    public static final String PT60SName = "beeCacheManager" + PT60S;

//    @Bean
//    @Primary
    public CacheManager beeCacheManager(
            @Qualifier(RedisMQService.NAME) MQService mqService
            , @Value("${cache.notify.topic:default}") String topic
    ) {
        return new BeeCacheManager(new BeeCacheMQServiceImpl(mqService, topic));
    }

//    @Bean(PT60SName)
    public CacheManager PT1D(
            @Qualifier(RedisMQService.NAME) MQService mqService
            , @Value("${cache.notify.topic:default}") String topic
    ) {
        return new BusyBeeCacheManager(new BeeCacheMQServiceImpl(mqService, topic + PT60S), PT60S);
    }

    @Bean(PT60SName)
    public CacheManager PT1D(
    ) {
        return new ConcurrentMapCacheManager();
    }
}
