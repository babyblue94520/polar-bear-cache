package pers.clare.polarbearcache.processor;

import pers.clare.polarbearcache.annotation.CacheAlive;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheAnnotationFactory implements BeanPostProcessor {
    private final ConcurrentMap<Class<?>, Object> resolvedClassMap = new ConcurrentHashMap<>();
    private final Map<String, CacheAliveConfig> cacheAliveMap = new HashMap<>();
    private final Map<Method, List<CachePutConfig>> cachePutMap = new HashMap<>();

    public CacheAliveConfig getCacheAlive(String name) {
        return cacheAliveMap.get(name);
    }

    public List<CachePutConfig> getCachePuts(Method method) {
        return cachePutMap.get(method);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        this.parse(bean.getClass());
        return bean;
    }

    private void parse(Class<?> clazz) {
        if (!clazz.getName().contains("$$")) {
            Object obj = new Object();
            if (resolvedClassMap.computeIfAbsent(clazz, (key) -> obj) != obj) return;
            CacheAlive cacheAlive = clazz.getDeclaredAnnotation(CacheAlive.class);
            parseCacheAlive(cacheAlive, clazz);
            int modifier;
            for (Method method : clazz.getDeclaredMethods()) {
                modifier = method.getModifiers();
                if (!Modifier.isPublic(modifier)
                        || Modifier.isStatic(modifier) || Modifier.isNative(modifier)
                ) continue;
                this.parseCacheAlive(cacheAlive, method);
                this.parseCachePut(method);
            }
        }
        Class<?> superClazz = clazz.getSuperclass();
        if (isSimpleType(superClazz)) return;
        parse(superClazz);
    }

    private void parseCacheAlive(CacheAlive cacheAlive, Class<?> clazz) {
        CacheConfig cacheConfig = clazz.getDeclaredAnnotation(CacheConfig.class);
        if (cacheConfig == null) return;
        CacheAliveConfig cacheAliveConfig = buildCacheAliveConfig(cacheAlive);
        if (cacheAliveConfig == null) return;
        addCacheAlive(cacheConfig.cacheNames(), cacheAliveConfig);
    }

    private void parseCacheAlive(CacheAlive classCacheAlive, Method method) {
        Cacheable cacheable = method.getDeclaredAnnotation(Cacheable.class);
        Caching caching = method.getDeclaredAnnotation(Caching.class);
        if (cacheable == null && caching == null) return;

        CacheAliveConfig cacheAliveConfig = buildCacheAliveConfig(classCacheAlive, method.getDeclaredAnnotation(CacheAlive.class));
        if (cacheAliveConfig == null) return;

        addCacheAlive(cacheable, cacheAliveConfig);
        if (caching == null) return;
        for (Cacheable cacheable2 : caching.cacheable()) {
            addCacheAlive(cacheable2, cacheAliveConfig);
        }
    }

    public void addCacheAlive(Cacheable cacheable, CacheAliveConfig cacheAliveConfig) {
        if (cacheable == null) return;
        addCacheAlive(cacheable.value(), cacheAliveConfig);
        addCacheAlive(cacheable.cacheNames(), cacheAliveConfig);
    }

    public void addCacheAlive(String[] names, CacheAliveConfig cacheAliveConfig) {
        if (names.length == 0) return;
        for (String name : names)
            cacheAliveMap.put(name, cacheAliveConfig);
    }

    private void parseCachePut(Method method) {
        CacheConfig config = method.getDeclaringClass().getDeclaredAnnotation(CacheConfig.class);
        String[] defaultNames = new String[]{};
        if (config != null) {
            defaultNames = config.cacheNames();
        }

        this.putCachePut(method, method.getDeclaredAnnotation(CachePut.class), defaultNames);

        Caching caching = method.getDeclaredAnnotation(Caching.class);
        if (caching == null || caching.put().length == 0) return;
        for (CachePut put : caching.put()) {
            this.putCachePut(method, put, defaultNames);
        }
    }

    public void putCachePut(Method method, CachePut cachePut, String[] defaultNames) {
        if (cachePut == null) return;
        String[] cacheNames = cachePut.cacheNames().length == 0 ? defaultNames : cachePut.cacheNames();
        if (cacheNames.length == 0) return;
        CachePutConfig config = new CachePutConfig(
                cacheNames
                , cachePut.key()
        );
        cachePutMap.computeIfAbsent(method, (k) -> new ArrayList<>()).add(config);
    }

    private CacheAliveConfig buildCacheAliveConfig(CacheAlive classCacheAlive) {
        return buildCacheAliveConfig(classCacheAlive, null);
    }

    private CacheAliveConfig buildCacheAliveConfig(CacheAlive classCacheAlive, CacheAlive methodCacheAlive) {
        CacheAlive cacheAlive = methodCacheAlive == null ? classCacheAlive : methodCacheAlive;
        if (cacheAlive == null) return null;
        long effectiveTime = StringUtils.hasLength(cacheAlive.value()) ? DurationStyle.detect(cacheAlive.value()).parse(cacheAlive.value()).toMillis() : 0;
        return new CacheAliveConfig(
                effectiveTime
                , cacheAlive.extension()
        );
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type.getName().startsWith("java.");
    }
}
