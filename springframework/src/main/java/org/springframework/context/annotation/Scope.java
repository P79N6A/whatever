package org.springframework.context.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

    @AliasFor("scopeName") String value() default "";

    @AliasFor("value") String scopeName() default "";

    ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

}
