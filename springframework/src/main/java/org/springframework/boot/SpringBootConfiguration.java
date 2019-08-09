package org.springframework.boot;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration(proxyBeanMethods = false)
public @interface SpringBootConfiguration {

    @AliasFor(annotation = Configuration.class) boolean proxyBeanMethods() default true;

}
