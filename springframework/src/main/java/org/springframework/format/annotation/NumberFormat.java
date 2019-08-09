package org.springframework.format.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface NumberFormat {

    Style style() default Style.DEFAULT;

    String pattern() default "";

    enum Style {

        DEFAULT,

        NUMBER,

        PERCENT,

        CURRENCY
    }

}
