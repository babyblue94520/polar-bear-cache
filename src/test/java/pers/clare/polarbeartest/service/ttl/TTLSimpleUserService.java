package pers.clare.polarbeartest.service.ttl;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pers.clare.polarbearcache.annotation.CacheAlive;
import pers.clare.polarbearcache.impl.PolarBearCacheDependenciesImpl;
import pers.clare.polarbeartest.cache.CoreCacheConfig;
import pers.clare.polarbeartest.service.AbstractSimpleUserService;
import pers.clare.polarbeartest.vo.SimpleUser;

import static pers.clare.polarbeartest.cache.key.TTLCacheKey.SimpleUser;
import static pers.clare.polarbeartest.cache.key.TTLCacheKey.User;

@Service
public class TTLSimpleUserService extends AbstractSimpleUserService implements InitializingBean {

    @Autowired
    private PolarBearCacheDependenciesImpl cacheDependencies;

    @Override
    public void afterPropertiesSet() {
        cacheDependencies.depend(SimpleUser, User);
    }

    @Cacheable(
            cacheNames = SimpleUser
            , key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    @CacheAlive(value = CoreCacheConfig.DurationValue)
    public SimpleUser find(Long id) {
        return super.find(id);
    }
}
