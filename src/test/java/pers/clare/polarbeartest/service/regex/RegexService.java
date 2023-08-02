package pers.clare.polarbeartest.service.regex;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pers.clare.polarbearcache.impl.CacheDependenciesImpl;

@Service
public class RegexService implements InitializingBean {
    private static final String RegexOne = "RegexOne";
    private static final String RegexMap = "RegexMap";

    private static final String RegexGroup = "RegexGroup";

    @Autowired
    private CacheDependenciesImpl cacheDependencies;

    @Override
    public void afterPropertiesSet() throws Exception {
        cacheDependencies.depend(RegexMap, (key) -> {
            if (key.startsWith("regex:^[^-]+-")) {
                return key;
            }else{
                String[] array = key.split("-");
                return array[0] + '-' + array[1];
            }
        }, RegexOne);

        cacheDependencies.depend(RegexGroup, (key) -> {

            if (key.startsWith("regex:^[^-]+-")) {
                return "regex:^[^-]+";
            } else {
                String[] array = key.split("-");
                return array[0];
            }
        }, RegexOne);
    }

    @Cacheable(
            cacheNames = RegexOne
            , key = "#group+'-'+#type+'-'+#key"
    )
    public Object find(String group, String type, String key) {
        return new Object();
    }

    @Cacheable(
            cacheNames = RegexMap
            , key = "#group+'-'+#type"
    )
    public Object findMap(String group, String type) {
        return new Object();
    }

    @Cacheable(
            cacheNames = RegexGroup
            , key = "#group"
    )
    public Object findGroup(String group) {
        return new Object();
    }

    @CacheEvict(
            cacheNames = RegexOne
            , key = "#group+'-'+#type+'-'+#key"
    )
    public void update(String group, String type, String key) {

    }

    @CacheEvict(
            cacheNames = RegexOne
            , key = "'regex:^'+#group+'-'+#type"
    )
    public void batch(String group, String type) {

    }

    @CacheEvict(
            cacheNames = RegexOne
            , key = "'regex:^[^-]+-'+#type"
    )
    public void batch2(String type) {

    }
}
