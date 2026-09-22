package pers.clare.polarbearcache.aop;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbearcache.annotation.CacheAlive;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MergedCacheAnnotationTest {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @CachePut
    public @interface ComposedPut {
        @AliasFor(annotation = CachePut.class, attribute = "cacheNames")
        String[] value() default {};
    }

    @CacheConfig(cacheNames = "defaults", cacheManager = "named")
    public interface Operations {
        @ComposedPut("composed")
        String put(String key);
        @Cacheable
        @CacheAlive("5s")
        String read(String key);
    }

    public static class Service implements Operations {
        public String put(String key) { return key; }
        public String read(String key) { return key; }
    }

    @Test
    void interfaceComposedAnnotationNotifiesThroughActualProxy() throws Exception {
        CacheAnnotationFactory factory = new CacheAnnotationFactory();
        Service target = new Service();
        factory.postProcessBeforeInitialization(target, "service");
        assertEquals(5000, factory.getCacheAlive("defaults").getEffectiveTime());
        PolarBearCacheManager manager = mock(PolarBearCacheManager.class);
        PolarBearCache cache = mock(PolarBearCache.class);
        when(manager.getCache("composed")).thenReturn(cache);
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("named", manager);
        CachePutAop advisor = new CachePutAop(new PolarBearCacheManager[]{manager}, factory, beans);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvisor(advisor);
        ((Operations) proxy.getProxy()).put("key");
        verify(cache).putNotify("key");
    }
}
