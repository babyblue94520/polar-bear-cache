package pers.clare.polarbearcache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

@SuppressWarnings("SpringFacetCodeInspection")
@Import({PolarBearCacheProperties.class, CacheAnnotationFactory.class})
@EnableCaching
@Configuration
public class PolarBearCacheConfiguration {

}
