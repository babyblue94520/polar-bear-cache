package pers.clare.polarbeartest.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheEventService;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbearcache.PolarBearCacheProperties;
import pers.clare.polarbearcache.event.EventSender;
import pers.clare.polarbearcache.impl.BasicCacheManager;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.time.Duration;

@Configuration
public class CoreCacheConfig {
    public static final String DurationValue = "PT1S";
    public static final String DurationValue2 = "PT2S";
    public static final long EffectiveTime = Duration.parse(DurationValue).toMillis();
    public static final long EffectiveTime2 = Duration.parse(DurationValue2).toMillis();
    @Bean
    public PolarBearCacheEventService polarBearCacheEventService() {
        return new CacheEventServiceImpl();
    }

    @Bean
    @Primary
    public PolarBearCacheManager polarBearCacheManager(
            CacheAnnotationFactory cacheAnnotationFactory
            , PolarBearCacheProperties properties
            , PolarBearCacheDependencies cacheDependencies
            , EventSender eventSender
    ) {
        return new BasicCacheManager(cacheAnnotationFactory, properties, cacheDependencies, eventSender);
    }
}
