package pers.clare.polarbearcache.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.*;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import pers.clare.polarbearcache.PolarBearCache;
import pers.clare.polarbearcache.PolarBearCacheManager;
import pers.clare.polarbearcache.proccessor.CacheAnnotationFactory;
import pers.clare.polarbearcache.proccessor.CachePutConfig;

import java.lang.reflect.Method;
import java.util.*;

@Order
public class CachePutAop extends StaticMethodMatcherPointcutAdvisor {
    private static final Logger log = LoggerFactory.getLogger(CachePutAop.class);
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final PolarBearCacheManager[] cacheManagers;

    private final CacheAnnotationFactory cacheAnnotationFactory;

    private final KeyGenerator keyGenerator;

    private final BeanFactory beanFactory;

    public CachePutAop(
            PolarBearCacheManager[] cacheManagers
            , CacheAnnotationFactory cacheAnnotationFactory
            , BeanFactory beanFactory
    ) {
        this(cacheManagers, cacheAnnotationFactory, beanFactory, null);
    }

    public CachePutAop(
            PolarBearCacheManager[] cacheManagers
            , CacheAnnotationFactory cacheAnnotationFactory
            , BeanFactory beanFactory
            , @Nullable KeyGenerator keyGenerator
    ) {
        this.cacheManagers = cacheManagers;
        this.cacheAnnotationFactory = cacheAnnotationFactory;
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory");
        this.keyGenerator = keyGenerator == null ? new SimpleKeyGenerator() : keyGenerator;
        setAdvice((MethodInterceptor) invocation -> {
            Object result = invocation.proceed();
            process(invocation.getThis(), invocation.getMethod(), invocation.getArguments(), result);
            return result;
        });
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return cacheAnnotationFactory.getCachePuts(method, targetClass) != null;
    }

    public void cachePut(JoinPoint joinPoint, Object result) {
        this.process(joinPoint, result);
    }

    public void caching(JoinPoint joinPoint, Object result) {
        this.process(joinPoint, result);
    }

    private void process(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object target = joinPoint.getTarget();
        process(target, signature.getMethod(), joinPoint.getArgs(), result);
    }

    private void process(Object target, Method invokedMethod, Object[] args, Object result) {
        Method method = AopUtils.getMostSpecificMethod(invokedMethod, target.getClass());
        List<CachePutConfig> cachePutConfigs = cacheAnnotationFactory.getCachePuts(invokedMethod, target.getClass());
        if (cachePutConfigs == null) return;

        for (CachePutConfig cachePutConfig : cachePutConfigs) {
            try {
                CachePutOperation operation = cachePutConfig.getOperation();
                ResolvedCaches resolvedCaches = resolveCaches(operation, target, method, args);
                EvaluationContext context = new MethodBasedEvaluationContext(
                        new CacheExpressionRoot(target, method, args, resolvedCaches.caches)
                        , method
                        , args
                        , parameterNameDiscoverer
                );
                context.setVariable("result", result);

                if (StringUtils.hasLength(operation.getCondition())
                        && !Boolean.TRUE.equals(parser.parseExpression(operation.getCondition()).getValue(context, Boolean.class))
                ) continue;
                if (StringUtils.hasLength(operation.getUnless())
                        && Boolean.TRUE.equals(parser.parseExpression(operation.getUnless()).getValue(context, Boolean.class))
                ) continue;

                Object generatedKey;
                if (StringUtils.hasLength(operation.getKey())) {
                    generatedKey = parser.parseExpression(operation.getKey()).getValue(context);
                } else {
                    generatedKey = resolveKeyGenerator(operation).generate(target, method, args);
                }
                if (generatedKey == null) throw new IllegalArgumentException("Null cache key returned.");

                String key = String.valueOf(generatedKey);
                for (Cache cache : resolvedCaches.caches) {
                    if (cache instanceof PolarBearCache) {
                        ((PolarBearCache) cache).putNotify(key);
                    }
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    private KeyGenerator resolveKeyGenerator(CachePutOperation operation) {
        if (!StringUtils.hasLength(operation.getKeyGenerator())) return keyGenerator;
        return beanFactory.getBean(operation.getKeyGenerator(), KeyGenerator.class);
    }

    private ResolvedCaches resolveCaches(CachePutOperation operation, Object target, Method method, Object[] args) {
        if (!StringUtils.hasLength(operation.getCacheResolver())) {
            Set<String> names = operation.getCacheNames();
            List<Cache> caches = new ArrayList<>(names.size());
            CacheManager specifiedManager = resolveCacheManager(operation);
            for (String name : names) {
                Cache cache = specifiedManager == null ? getCache(name) : specifiedManager.getCache(name);
                if (cache != null) caches.add(cache);
            }
            return new ResolvedCaches(caches);
        }
        CacheResolver resolver = beanFactory.getBean(operation.getCacheResolver(), CacheResolver.class);
        Collection<? extends Cache> caches = resolver.resolveCaches(
                new CachePutInvocationContext(operation, target, method, args)
        );
        return new ResolvedCaches(caches);
    }

    @Nullable
    private CacheManager resolveCacheManager(CachePutOperation operation) {
        if (!StringUtils.hasLength(operation.getCacheManager())) return null;
        return beanFactory.getBean(operation.getCacheManager(), CacheManager.class);
    }

    @Nullable
    private Cache getCache(String name) {
        for (PolarBearCacheManager cacheManager : cacheManagers) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) return cache;
        }
        return null;
    }

    private static class ResolvedCaches {
        private final Collection<? extends Cache> caches;

        private ResolvedCaches(Collection<? extends Cache> caches) {
            this.caches = Collections.unmodifiableCollection(new ArrayList<>(caches));
        }
    }

    private static class CachePutInvocationContext implements CacheOperationInvocationContext<CachePutOperation> {
        private final CachePutOperation operation;
        private final Object target;
        private final Method method;
        private final Object[] args;

        private CachePutInvocationContext(CachePutOperation operation, Object target, Method method, Object[] args) {
            this.operation = operation;
            this.target = target;
            this.method = method;
            this.args = args;
        }

        @Override
        public CachePutOperation getOperation() {
            return operation;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }
    }

    @SuppressWarnings("unused")
    private static class CacheExpressionRoot {
        private final Object target;
        private final Method method;
        private final Object[] args;
        private final Collection<? extends Cache> caches;

        private CacheExpressionRoot(
                Object target
                , Method method
                , Object[] args
                , Collection<? extends Cache> caches
        ) {
            this.target = target;
            this.method = method;
            this.args = args;
            this.caches = caches;
        }

        public Object getTarget() {
            return target;
        }

        public Class<?> getTargetClass() {
            return target.getClass();
        }

        public Method getMethod() {
            return method;
        }

        public String getMethodName() {
            return method.getName();
        }

        public Object[] getArgs() {
            return args;
        }

        public Collection<? extends Cache> getCaches() {
            return caches;
        }
    }
}
