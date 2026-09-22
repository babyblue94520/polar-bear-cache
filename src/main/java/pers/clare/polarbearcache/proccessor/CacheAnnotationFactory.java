package pers.clare.polarbearcache.proccessor;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import pers.clare.polarbearcache.annotation.CacheAlive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAnnotationFactory implements BeanPostProcessor {
    private final AnnotationCacheOperationSource operationSource = new AnnotationCacheOperationSource();
    private final Map<String, CacheAliveConfig> cacheAliveMap = new ConcurrentHashMap<>();

    public CacheAliveConfig getCacheAlive(String name) {
        return cacheAliveMap.get(name);
    }

    public List<CachePutConfig> getCachePuts(Method method) {
        return getCachePuts(method, method.getDeclaringClass());
    }

    public List<CachePutConfig> getCachePuts(Method method, Class<?> targetClass) {
        Collection<CacheOperation> operations = operationSource.getCacheOperations(method, targetClass);
        if (operations == null) return null;
        List<CachePutConfig> puts = new ArrayList<>();
        for (CacheOperation operation : operations) {
            if (operation instanceof CachePutOperation) {
                puts.add(new CachePutConfig((CachePutOperation) operation));
            }
        }
        return puts.isEmpty() ? null : puts;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        CacheAlive classAlive = AnnotatedElementUtils.findMergedAnnotation(targetClass, CacheAlive.class);
        for (Method method : targetClass.getMethods()) {
            Collection<CacheOperation> operations = operationSource.getCacheOperations(method, targetClass);
            if (operations == null) continue;
            CacheAlive alive = AnnotatedElementUtils.findMergedAnnotation(method, CacheAlive.class);
            if (alive == null) alive = classAlive;
            if (alive == null) continue;
            long ttl = StringUtils.hasLength(alive.value())
                    ? DurationStyle.detectAndParse(alive.value()).toMillis() : 0;
            CacheAliveConfig config = new CacheAliveConfig(ttl, alive.extension());
            for (CacheOperation operation : operations) {
                if (operation instanceof CacheableOperation) {
                    for (String name : operation.getCacheNames()) cacheAliveMap.put(name, config);
                }
            }
        }
        return bean;
    }
}
