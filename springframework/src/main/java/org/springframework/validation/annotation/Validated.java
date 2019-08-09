package org.springframework.validation.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {

    Class<?>[] value() default {};

}
