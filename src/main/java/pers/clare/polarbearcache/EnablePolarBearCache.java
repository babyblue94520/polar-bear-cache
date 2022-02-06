package pers.clare.polarbearcache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({PolarBearCacheConfiguration.class})
@EnableConfigurationProperties
@SuppressWarnings("unused")
public @interface EnablePolarBearCache {
    String value() default "";
}
