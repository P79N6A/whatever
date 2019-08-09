package org.springframework.context.event;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

    @AliasFor("classes") Class<?>[] value() default {};

    @AliasFor("value") Class<?>[] classes() default {};

    String condition() default "";

}
