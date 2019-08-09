package org.springframework.context.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

    @AliasFor(annotation = Component.class) String value() default "";

    /**
     * 默认代理Bean方法
     */
    boolean proxyBeanMethods() default true;

}
