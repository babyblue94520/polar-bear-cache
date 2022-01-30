package pers.clare.polarbearcache.aop;

import pers.clare.polarbearcache.CompositePolarBearCacheManager;
import pers.clare.polarbearcache.processor.CacheAnnotationFactory;
import pers.clare.polarbearcache.processor.CachePutConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.util.List;

@Aspect
@Order
public class CachePutAop {
    private static final Logger log = LogManager.getLogger();
    private final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    private CompositePolarBearCacheManager cacheManager;

    @Autowired
    private CacheAnnotationFactory cacheAnnotationFactory;

    @AfterReturning(value = "@annotation(org.springframework.cache.annotation.CachePut)", returning = "result")
    public void cachePut(JoinPoint joinPoint, Object result) {
        this.process(joinPoint, result);
    }

    @AfterReturning(value = "@annotation(org.springframework.cache.annotation.Caching)", returning = "result")
    public void caching(JoinPoint joinPoint, Object result) {
        this.process(joinPoint, result);
    }

    private void process(JoinPoint joinPoint, Object result) {
        List<CachePutConfig> cachePutConfigs = cacheAnnotationFactory.getCachePuts(((MethodSignature) joinPoint.getSignature()).getMethod());
        if (cachePutConfigs == null) return;

        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        context.setVariable("result", result);
        for (int i = 0; i < args.length; i++) {
            context.setVariable(names[i], args[i]);
        }

        for (CachePutConfig cachePutConfig : cachePutConfigs) {
            String key = cachePutConfig.getKey();
            if (StringUtils.hasLength(key)) {
                try {
                    key = String.valueOf(parser.parseExpression(key).getValue(context));
                    for (String name : cachePutConfig.getCacheNames()) {
                        cacheManager.evictDependents(name, key);
                        cacheManager.evictNotify(name, key);
                    }
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            } else {
                for (String name : cachePutConfig.getCacheNames()) {
                    cacheManager.clearDependents(name);
                    cacheManager.clearNotify(name);
                }
            }
        }
    }
}
