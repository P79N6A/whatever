package org.apache.ibatis.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UpdateProvider {

    Class<?> type();

    String method() default "";

}
