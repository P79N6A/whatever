package org.springframework.context.annotation;

import org.springframework.core.io.support.PropertySourceFactory;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

    String name() default "";

    String[] value();

    boolean ignoreResourceNotFound() default false;

    String encoding() default "";

    Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
