package pers.clare.polarbeartest.service.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pers.clare.polarbearcache.impl.PolarBearCacheDependenciesImpl;
import pers.clare.polarbeartest.service.AbstractSimpleUserService;
import pers.clare.polarbeartest.vo.SimpleUser;

import static pers.clare.polarbeartest.cache.key.CacheConfigCacheKey.SimpleUser;
import static pers.clare.polarbeartest.cache.key.CacheConfigCacheKey.User;

@CacheConfig(
        cacheNames = SimpleUser
)
@Service
public class CacheConfigSimpleUserService extends AbstractSimpleUserService implements InitializingBean {

    @Autowired
    private PolarBearCacheDependenciesImpl cacheDependencies;

    @Override
    public void afterPropertiesSet() {
        cacheDependencies.depend(SimpleUser, User);
    }


    @Cacheable(
            key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public SimpleUser find(Long id) {
        return super.find(id);
    }
}
