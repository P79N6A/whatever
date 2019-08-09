package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SessionAttributes {

    @AliasFor("names") String[] value() default {};

    @AliasFor("value") String[] names() default {};

    Class<?>[] types() default {};

}
