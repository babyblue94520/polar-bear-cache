package pers.clare.polarbeartest.service.config;

import org.springframework.stereotype.Service;
import pers.clare.polarbeartest.cache.CacheType;
import pers.clare.polarbeartest.service.ServiceContext;

@Service
public class CacheConfigContext extends ServiceContext<CacheConfigUserService, CacheConfigSimpleUserService> {

    @Override
    protected CacheType type() {
        return CacheType.CacheConfig;
    }
}
