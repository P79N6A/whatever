package org.apache.dubbo.common;

import java.lang.annotation.*;

@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Extension {

    @Deprecated String value() default "";

}