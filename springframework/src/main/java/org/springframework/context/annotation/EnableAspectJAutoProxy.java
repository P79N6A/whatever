package org.springframework.context.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class) // 引入
public @interface EnableAspectJAutoProxy {

    boolean proxyTargetClass() default false;

    boolean exposeProxy() default false;

}
