package org.springframework.beans.factory.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configurable {

    String value() default "";

    Autowire autowire() default Autowire.NO;

    boolean dependencyCheck() default false;

    boolean preConstruction() default false;

}
