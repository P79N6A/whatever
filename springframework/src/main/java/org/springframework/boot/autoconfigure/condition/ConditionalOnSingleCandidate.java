package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnSingleCandidate {

    Class<?> value() default Object.class;

    String type() default "";

    SearchStrategy search() default SearchStrategy.ALL;

}
