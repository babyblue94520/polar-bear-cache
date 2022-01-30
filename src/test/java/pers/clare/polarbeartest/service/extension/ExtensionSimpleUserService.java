package pers.clare.polarbeartest.service.extension;

import pers.clare.polarbearcache.annotation.CacheAlive;
import pers.clare.polarbearcache.impl.PolarBearCacheDependenciesImpl;
import pers.clare.polarbeartest.cache.CoreCacheConfig;
import pers.clare.polarbeartest.service.AbstractSimpleUserService;
import pers.clare.polarbeartest.vo.SimpleUser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static pers.clare.polarbeartest.cache.key.ExtensionCacheKey.*;

@Service
public class ExtensionSimpleUserService extends AbstractSimpleUserService implements InitializingBean {

    @Autowired
    private PolarBearCacheDependenciesImpl cacheDependencies;

    @Override
    public void afterPropertiesSet() {
        cacheDependencies.depend(SimpleUser, User);
    }


    @CacheAlive(value = CoreCacheConfig.DurationValue, extension = true)
    @Cacheable(
            cacheNames = SimpleUser
            , key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public SimpleUser find(Long id) {
        return super.find(id);
    }
}
