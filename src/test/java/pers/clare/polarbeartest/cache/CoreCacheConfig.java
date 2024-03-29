package pers.clare.polarbeartest.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pers.clare.polarbearcache.PolarBearCacheEventService;

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

}
