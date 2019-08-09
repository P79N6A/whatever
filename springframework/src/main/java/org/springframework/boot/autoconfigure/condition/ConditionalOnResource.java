package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnResourceCondition.class)
public @interface ConditionalOnResource {

    String[] resources() default {};

}
