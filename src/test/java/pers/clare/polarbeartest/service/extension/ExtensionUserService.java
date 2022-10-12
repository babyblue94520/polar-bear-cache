package pers.clare.polarbeartest.service.extension;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import pers.clare.polarbearcache.PolarBearCacheDependencies;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbearcache.annotation.CacheAlive;
import pers.clare.polarbeartest.cache.CoreCacheConfig;
import pers.clare.polarbeartest.service.AbstractUserService;
import pers.clare.polarbeartest.vo.ReloadUser;
import pers.clare.polarbeartest.vo.User;

import java.util.List;

import static pers.clare.polarbeartest.cache.key.ExtensionCacheKey.*;

@Service
@CacheAlive(value = CoreCacheConfig.DurationValue2)
public class ExtensionUserService extends AbstractUserService implements InitializingBean {

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

    @CacheAlive(value = CoreCacheConfig.DurationValue, extension = true)
    @Cacheable(
            cacheNames = User
            , key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public User find(Long id) {
        return super.find(id);
    }

    @CacheAlive(value = CoreCacheConfig.DurationValue, extension = true)
    @Cacheable(
            cacheNames = ReloadUser
            , key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public ReloadUser findReload(Long id) {
        return super.findReload(id);
    }

    @CacheAlive(value = CoreCacheConfig.DurationValue, extension = true)
    @Cacheable(
            cacheNames = User
            , key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public User findNull(Long id) {
        return null;
    }

    @CacheAlive(value = CoreCacheConfig.DurationValue, extension = true)
    @Cacheable(
            cacheNames = UserSync
            , key = "#id"
            , condition = "#id !=null"
            , sync = true
    )
    public User findSync(Long id) {
        return super.findSync(id);
    }

    @CacheEvict(
            cacheNames = User
            , key = "''"
    )
    public User insert() {
        return super.insert();
    }

    @CacheEvict(
            cacheNames = User
            , key = "#id"
            , condition = "#id !=null"
    )
    public User update(Long id) {
        return super.update(id);
    }

    @CachePut(
            cacheNames = User
            , key = "#id"
            , condition = "#id !=null"
            , unless = "#result==null"
    )
    public User updatePut(Long id) {
        return super.updatePut(id);
    }

    @CachePut(
            cacheNames = User
            , key = "#result.id"
            , condition = "#result!=null&&#result.id !=null"
            , unless = "#result==null"
    )
    public User updatePutResultKey(Long id) {
        return super.updatePut(id);
    }

    @Caching(
            cacheable = {
                    @Cacheable(
                            cacheNames = User
                            , key = "#id"
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
                            cacheNames = User
                            , key = "#id"
                            , condition = "#id !=null"
                            , unless = "#result==null"
                    )
            }
    )
    public User updateCachingPut(Long id) {
        return super.updatePut(id);
    }
}
