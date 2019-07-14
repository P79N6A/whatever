package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.URL;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activate {

    String[] group() default {};

    String[] value() default {};

    @Deprecated String[] before() default {};

    @Deprecated String[] after() default {};

    int order() default 0;

}