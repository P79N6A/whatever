package org.springframework.boot.context.properties;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NestedConfigurationProperty {

}
