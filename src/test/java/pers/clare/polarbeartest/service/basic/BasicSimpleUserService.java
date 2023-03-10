package pers.clare.polarbeartest.service.basic;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pers.clare.polarbearcache.impl.CacheDependenciesImpl;
import pers.clare.polarbeartest.service.AbstractSimpleUserService;
import pers.clare.polarbeartest.vo.SimpleUser;

import static pers.clare.polarbeartest.cache.key.BasicCacheKey.SimpleUser;
import static pers.clare.polarbeartest.cache.key.BasicCacheKey.User;

@Service
public class BasicSimpleUserService extends AbstractSimpleUserService implements InitializingBean {

    @Autowired
    private CacheDependenciesImpl cacheDependencies;

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
    public SimpleUser find(Long id) {
        return super.find(id);
    }
}
