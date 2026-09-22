package pers.clare.polarbearcache;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import pers.clare.polarbearcache.aop.CachePutAop;
import pers.clare.polarbearcache.impl.BasicCacheManager;
import pers.clare.polarbearcache.impl.CacheDependenciesImpl;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

@ConditionalOnBean(PolarBearCacheConfiguration.class)
@AutoConfigureBefore(CacheAutoConfiguration.class)
public class PolarBearCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CachePutAop.class)
    public CachePutAop cachePutAop(
            PolarBearCacheManager[] cacheManagers
            , CacheAnnotationFactory cacheAnnotationFactory
            , ObjectProvider<KeyGenerator> keyGenerators
            , BeanFactory beanFactory
    ) {
        return new CachePutAop(cacheManagers, cacheAnnotationFactory, beanFactory, keyGenerators.getIfUnique());
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
            , @Nullable PolarBearCacheEventService eventService
    ) {
        return new BasicCacheManager(
                cacheAnnotationFactory
                , properties
                , cacheDependencies
                , eventService
        );
    }

}
