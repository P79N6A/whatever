package org.springframework.core.annotation;

import org.springframework.core.Ordered;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {

    int value() default Ordered.LOWEST_PRECEDENCE;

}
