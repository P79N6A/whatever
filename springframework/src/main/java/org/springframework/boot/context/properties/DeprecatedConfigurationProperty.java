package org.springframework.boot.context.properties;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeprecatedConfigurationProperty {

    String reason() default "";

    String replacement() default "";

}
