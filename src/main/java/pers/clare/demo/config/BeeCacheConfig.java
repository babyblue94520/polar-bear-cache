package pers.clare.demo.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pers.clare.core.cache.BeeCacheContext;
import pers.clare.core.cache.BeeCacheManager;
import pers.clare.core.cache.busy.BusyBeeCacheManager;
import pers.clare.core.cache.expire.ExpireBeeCacheManager;
import pers.clare.core.mq.MQService;
import pers.clare.redis.RedisMQService;

@Configuration
public class BeeCacheConfig implements InitializingBean {
    private static final String PT60S = "PT60S";

    // 闲置 60 秒清除 Cache
    public static final String ExpirePT60SName = "expireBeeCacheManager" + PT60S;
    // 闲置 60 秒清除 Cache
    public static final String BusyPT60SName = "busyBeeCacheManager" + PT60S;

    @Autowired
    @Qualifier(RedisMQService.NAME)
    private MQService mqService;

    @Override
    public void afterPropertiesSet() throws Exception {
        mqService.onConnected(BeeCacheManager::clearAll);
    }

    @Bean
    @Primary
    public CacheManager beeCacheManager(
            @Qualifier(RedisMQService.NAME) MQService mqService
            , @Value("${cache.notify.topic:default}") String topic
    ) {
        return new BeeCacheManager(new BeeCacheMQServiceImpl(mqService, topic));
    }

    @Bean(ExpirePT60SName)
    public CacheManager ExpirePT1D(
            @Qualifier(RedisMQService.NAME) MQService mqService
            , @Value("${cache.notify.topic:default}") String topic
    ) {
        return new ExpireBeeCacheManager(new BeeCacheMQServiceImpl(mqService, topic + "Expire" + PT60S), PT60S);
    }

    @Bean(BusyPT60SName)
    public CacheManager BusyPT1D(
            @Qualifier(RedisMQService.NAME) MQService mqService
            , @Value("${cache.notify.topic:default}") String topic
    ) {
        return new BusyBeeCacheManager(new BeeCacheMQServiceImpl(mqService, topic + "Busy" + PT60S), PT60S);
    }

    //    @Bean(PT60SName)
    public CacheManager PT1D(
    ) {
        return new ConcurrentMapCacheManager();
    }

}
