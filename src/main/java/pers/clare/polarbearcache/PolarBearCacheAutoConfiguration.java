package pers.clare.polarbearcache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import pers.clare.polarbearcache.impl.BasicCacheManager;
import pers.clare.polarbearcache.impl.CompositeCacheManagerImpl;
import pers.clare.polarbearcache.impl.PolarBearCacheDependenciesImpl;

public class PolarBearCacheAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(PolarBearCacheDependencies.class)
    public PolarBearCacheDependencies cacheDependencies() {
        return new PolarBearCacheDependenciesImpl();
    }

    @Bean
    @ConditionalOnMissingBean(PolarBearCacheManager.class)
    public PolarBearCacheManager cacheManager() {
        return new BasicCacheManager();
    }

    @Bean
    @ConditionalOnMissingBean(CompositePolarBearCacheManager.class)
    public CompositePolarBearCacheManager compositeCacheManager(
            PolarBearCacheManager... polarBearCacheManagers
    ) {
        return new CompositeCacheManagerImpl(polarBearCacheManagers);
    }

}
