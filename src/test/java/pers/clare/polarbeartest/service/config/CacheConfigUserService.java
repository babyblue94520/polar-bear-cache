package pers.clare.polarbeartest.service.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbeartest.service.AbstractUserService;
import pers.clare.polarbeartest.vo.ReloadUser;
import pers.clare.polarbeartest.vo.User;

import java.util.List;

import static pers.clare.polarbeartest.cache.key.CacheConfigCacheKey.*;

@CacheConfig(
        cacheNames = User
)
@Service
public class CacheConfigUserService extends AbstractUserService implements InitializingBean {

    @Autowired
    private PolarBearCacheDependencies cacheDependencies;

    @Autowired
    private PolarBearCacheManager cacheManager;

    @Override
    public void afterPropertiesSet() {
        cacheDependencies.depend(AllUser, true, User);
        cacheDependencies.depend(ReloadUser, (key) -> "1", User);
        cacheManager.<ReloadUser>onEvict(ReloadUser, (key, oldValue) -> reloadUser);
    }

    @Override
    public void clear() {
        cacheManager.clear(ReloadUser);
    }

    @Cacheable(
            cacheNames = AllUser
            , key = "'all'"
            , unless = "#result==null"
    )
    public List<User> findAll() {
        return super.findAll();
    }

    @Cacheable(
            key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public User find(Long id) {
        return super.find(id);
    }

    @Cacheable(
            key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public User findNull(Long id) {
        return null;
    }

    @Cacheable(
            cacheNames = UserSync
            , key = "#id"
            , condition = "#id !=null"
            , sync = true
    )
    public User findSync(Long id) {
        return super.findSync(id);
    }

    @Cacheable(
            cacheNames = ReloadUser
            , key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public ReloadUser findReload(Long id) {
        return super.findReload(id);
    }

    @CacheEvict(
            key = "''"
    )
    public User insert() {
        return super.insert();
    }

    @CacheEvict(
            key = "#id"
            , condition = "#id !=null"
    )
    public User update(Long id) {
        return super.update(id);
    }

    @CachePut(
            key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public User updatePut(Long id) {
        return super.updatePut(id);
    }

    @CachePut(
            key = "#result.id"
            , condition = "#result!=null&&#result.id !=null"
            , unless = "#result==null"
    )
    public User updatePutResultKey(Long id) {
        return super.updatePut(id);
    }

    @Caching(
            cacheable = {
                    @Cacheable(
                            key = "#id"
                            , condition = "#id !=null"
                            , unless = "#result==null"
                    )
            }
    )
    public User findCaching(Long id) {
        return super.find(id);
    }

    @Caching(
            put = {
                    @CachePut(
                            key = "#id"
                            , condition = "#id !=null"
                            , unless = "#result==null"
                    )
            }
    )
    public User updateCachingPut(Long id) {
        return super.updatePut(id);
    }
}
