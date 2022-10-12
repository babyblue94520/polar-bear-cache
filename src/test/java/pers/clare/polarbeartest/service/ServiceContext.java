package pers.clare.polarbeartest.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import pers.clare.polarbeartest.cache.CacheType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ServiceContext<UserService, SimpleUserService> implements InitializingBean {
    private static final Map<CacheType, List<Object>> userServiceTypes = new ConcurrentHashMap<>();
    private static final Map<CacheType, List<Object>> simpleUserServiceTypes = new ConcurrentHashMap<>();

    @Autowired
    private UserService userService;

    @Autowired
    private SimpleUserService simpleUserService;

    abstract protected CacheType type();

    @Override
    public void afterPropertiesSet() throws Exception {
        userServiceTypes.computeIfAbsent(type(), (key) -> new CopyOnWriteArrayList<>()).add(userService);
        simpleUserServiceTypes.computeIfAbsent(type(), (key) -> new CopyOnWriteArrayList<>()).add(simpleUserService);
    }

    public static <T> List<T> userServices(CacheType type) {
        return (List<T>) userServiceTypes.get(type);
    }

    public static <T> List<T> simpleUserServices(CacheType type) {
        return (List<T>)  simpleUserServiceTypes.get(type);
    }
}
