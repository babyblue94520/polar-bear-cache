package pers.clare.polarbearcache.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheAlive {
    /**
     * Duration value.
     */
    String value() default "";

    /**
     * Extend the effective time of each use.
     */
    boolean extension() default false;
}
