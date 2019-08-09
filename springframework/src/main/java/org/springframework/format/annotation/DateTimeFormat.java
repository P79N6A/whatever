package org.springframework.format.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DateTimeFormat {

    String style() default "SS";

    ISO iso() default ISO.NONE;

    String pattern() default "";

    enum ISO {

        DATE,

        TIME,

        DATE_TIME,

        NONE
    }

}
