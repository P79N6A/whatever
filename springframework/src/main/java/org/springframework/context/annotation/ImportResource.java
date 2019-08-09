package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ImportResource {

    @AliasFor("locations") String[] value() default {};

    @AliasFor("value") String[] locations() default {};

    Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
