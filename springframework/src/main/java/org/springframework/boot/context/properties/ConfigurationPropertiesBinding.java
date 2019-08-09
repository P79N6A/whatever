package org.springframework.boot.context.properties;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

@Qualifier(ConfigurationPropertiesBinding.VALUE)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationPropertiesBinding {

    String VALUE = "org.springframework.boot.context.properties.ConfigurationPropertiesBinding";

}
