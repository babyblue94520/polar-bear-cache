package pers.clare.polarbearcache.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CachePutAopTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void defaultKeyUsesSpringKeyGeneratorInsteadOfClearingCache() throws Exception {
        PolarBearCacheManager cacheManager = cacheManager();
        PolarBearCache cache = cache(cacheManager, "cache");
        CacheAnnotationFactory annotationFactory = factoryFor(new PutService());
        PutService target = new PutService();
        Method method = PutService.class.getMethod("put", String.class);

        cachePutAop(annotationFactory, cacheManager)
                .cachePut(joinPoint(target, method, "id"), "value");

        verify(cache).putNotify("id");
        verify(cacheManager, never()).clearDependents(anyString());
        verify(cacheManager, never()).clearNotify(anyString());
    }

    @Test
    void conditionAndUnlessCanSuppressNotification() throws Exception {
        PolarBearCacheManager cacheManager = cacheManager();
        PolarBearCache cache = cache(cacheManager, "cache");
        PutService target = new PutService();
        CacheAnnotationFactory annotationFactory = factoryFor(target);
        CachePutAop aop = cachePutAop(annotationFactory, cacheManager);

        Method conditional = PutService.class.getMethod("conditionalPut", String.class);
        aop.cachePut(joinPoint(target, conditional, new Object[]{null}), "value");

        Method unless = PutService.class.getMethod("unlessPut", String.class);
        aop.cachePut(joinPoint(target, unless, "id"), null);

        verify(cache, never()).putNotify(anyString());
    }

    @Test
    void namedKeyGeneratorIsUsed() throws Exception {
        PolarBearCacheManager cacheManager = cacheManager();
        PolarBearCache cache = cache(cacheManager, "cache");
        PutService target = new PutService();
        CacheAnnotationFactory annotationFactory = factoryFor(target);
        KeyGenerator namedGenerator = mock(KeyGenerator.class);
        when(namedGenerator.generate(any(), any(), any())).thenReturn("generated");
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("namedGenerator", namedGenerator);
        Method method = PutService.class.getMethod("namedKeyPut", String.class);

        new CachePutAop(new PolarBearCacheManager[]{cacheManager}, annotationFactory, beanFactory, null)
                .cachePut(joinPoint(target, method, "id"), "value");

        verify(namedGenerator).generate(target, method, "id");
        verify(cache).putNotify("generated");
    }

    @Test
    void namedCacheResolverDeterminesNotificationCache() throws Exception {
        PolarBearCacheManager cacheManager = cacheManager();
        PutService target = new PutService();
        CacheAnnotationFactory annotationFactory = factoryFor(target);
        CacheResolver resolver = mock(CacheResolver.class);
        PolarBearCache resolvedCache = mock(PolarBearCache.class);
        when(resolvedCache.getName()).thenReturn("resolved-cache");
        doReturn(Collections.singleton(resolvedCache)).when(resolver).resolveCaches(any());
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("namedResolver", resolver);
        Method method = PutService.class.getMethod("resolvedPut", String.class);

        new CachePutAop(new PolarBearCacheManager[]{cacheManager}, annotationFactory, beanFactory, null)
                .cachePut(joinPoint(target, method, "id"), "value");

        verify(resolvedCache).putNotify("id");
    }

    @Test
    void namedCacheManagerIsUsedForResolutionAndNotification() throws Exception {
        PolarBearCacheManager selected = cacheManager();
        PolarBearCacheManager other = cacheManager();
        PolarBearCache cache = mock(PolarBearCache.class);
        when(selected.getCache("cache")).thenReturn(cache);
        when(other.getCacheNames()).thenReturn(Collections.singleton("cache"));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("selectedCacheManager", selected);
        PutService target = new PutService();
        CacheAnnotationFactory annotationFactory = factoryFor(target);
        Method method = PutService.class.getMethod("namedManagerPut", String.class);

        new CachePutAop(
                new PolarBearCacheManager[]{other, selected}
                , annotationFactory
                , beanFactory
                , null
        ).cachePut(joinPoint(target, method, "id"), "value");

        verify(selected).getCache("cache");
        verify(cache).putNotify("id");
        verify(other, never()).getCache(anyString());
    }

    @Test
    void transactionHandlingIsDelegatedToCache() throws Exception {
        PolarBearCacheManager cacheManager = cacheManager();
        PolarBearCache cache = cache(cacheManager, "cache");
        PutService target = new PutService();
        CacheAnnotationFactory annotationFactory = factoryFor(target);
        Method method = PutService.class.getMethod("put", String.class);
        TransactionSynchronizationManager.initSynchronization();

        cachePutAop(annotationFactory, cacheManager)
                .cachePut(joinPoint(target, method, "id"), "value");

        verify(cache).putNotify("id");
        verify(cacheManager, never()).evictNotify(anyString(), anyString());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void implementationAnnotationIsFoundFromInterfaceMethod() throws Exception {
        PolarBearCacheManager cacheManager = cacheManager();
        PolarBearCache cache = cache(cacheManager, "interface-cache");
        InterfacePutService target = new InterfacePutService();
        CacheAnnotationFactory annotationFactory = factoryFor(target);
        Method interfaceMethod = PutOperations.class.getMethod("put", String.class);

        cachePutAop(annotationFactory, cacheManager)
                .cachePut(joinPoint(target, interfaceMethod, "id"), "value");

        verify(cache).putNotify("id");
    }

    @Test
    void onlyOwningManagerHandlesCachePutEvent() throws Exception {
        PolarBearCacheManager owner = cacheManager();
        PolarBearCacheManager other = cacheManager();
        PolarBearCache cache = cache(owner, "cache");
        PutService target = new PutService();
        CacheAnnotationFactory annotationFactory = factoryFor(target);
        Method method = PutService.class.getMethod("put", String.class);

        cachePutAop(annotationFactory, owner, other)
                .cachePut(joinPoint(target, method, "id"), "value");

        verify(cache).putNotify("id");
        verify(other, never()).getCache(anyString());
    }

    @Test
    void beanFactoryIsRequired() {
        assertThrows(NullPointerException.class, () -> new CachePutAop(
                new PolarBearCacheManager[0]
                , mock(CacheAnnotationFactory.class)
                , null
        ));
    }

    private static CacheAnnotationFactory factoryFor(Object target) {
        CacheAnnotationFactory factory = new CacheAnnotationFactory();
        factory.postProcessBeforeInitialization(target, "putService");
        return factory;
    }

    private static PolarBearCacheManager cacheManager() {
        return mock(PolarBearCacheManager.class);
    }

    private static CachePutAop cachePutAop(
            CacheAnnotationFactory annotationFactory
            , PolarBearCacheManager... cacheManagers
    ) {
        return new CachePutAop(cacheManagers, annotationFactory, new DefaultListableBeanFactory());
    }

    private static PolarBearCache cache(PolarBearCacheManager cacheManager, String name) {
        PolarBearCache cache = mock(PolarBearCache.class);
        when(cache.getName()).thenReturn(name);
        when(cacheManager.getCache(name)).thenReturn(cache);
        return cache;
    }

    private static JoinPoint joinPoint(Object target, Method method, Object... args) {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    public static class PutService {
        @CachePut(cacheNames = "cache")
        public String put(String id) {
            return id;
        }

        @CachePut(cacheNames = "cache", condition = "#id != null")
        public String conditionalPut(String id) {
            return id;
        }

        @CachePut(cacheNames = "cache", unless = "#result == null")
        public String unlessPut(String id) {
            return id;
        }

        @CachePut(cacheNames = "cache", keyGenerator = "namedGenerator")
        public String namedKeyPut(String id) {
            return id;
        }

        @CachePut(cacheResolver = "namedResolver")
        public String resolvedPut(String id) {
            return id;
        }

        @CachePut(cacheNames = "cache", cacheManager = "selectedCacheManager")
        public String namedManagerPut(String id) {
            return id;
        }
    }

    public interface PutOperations {
        String put(String id);
    }

    public static class InterfacePutService implements PutOperations {
        @Override
        @CachePut(cacheNames = "interface-cache")
        public String put(String id) {
            return id;
        }
    }
}
