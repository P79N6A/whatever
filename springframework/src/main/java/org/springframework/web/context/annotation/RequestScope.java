package org.springframework.web.context.annotation;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.context.WebApplicationContext;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(WebApplicationContext.SCOPE_REQUEST)
public @interface RequestScope {

    @AliasFor(annotation = Scope.class) ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
