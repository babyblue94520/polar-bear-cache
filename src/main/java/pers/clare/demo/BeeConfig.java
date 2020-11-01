package pers.clare.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pers.clare.core.cache.BeeCacheManager;
import pers.clare.core.cache.BeeCacheMQService;

@Configuration
public class BeeConfig {

    @Bean
    public BeeCacheManager beeCacheManager(
            BeeCacheMQService beeCacheMQService
    ){
        return new BeeCacheManager(beeCacheMQService);
    }
}
