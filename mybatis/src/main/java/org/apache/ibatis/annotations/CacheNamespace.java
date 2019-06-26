package org.apache.ibatis.annotations;

import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespace {
    Class<? extends org.apache.ibatis.cache.Cache> implementation() default PerpetualCache.class;

    Class<? extends org.apache.ibatis.cache.Cache> eviction() default LruCache.class;

    long flushInterval() default 0;

    int size() default 1024;

    boolean readWrite() default true;

    boolean blocking() default false;

    Property[] properties() default {};

}
