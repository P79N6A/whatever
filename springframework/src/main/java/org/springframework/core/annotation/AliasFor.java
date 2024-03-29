package org.springframework.core.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AliasFor {

    /**
     * attribute、value互为Alias
     */
    @AliasFor("attribute") String value() default "";

    @AliasFor("value") String attribute() default "";

    Class<? extends Annotation> annotation() default Annotation.class;

}
