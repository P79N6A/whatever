package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModelAttribute {

    @AliasFor("name") String value() default "";

    @AliasFor("value") String name() default "";

    boolean binding() default true;

}
