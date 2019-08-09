package org.springframework.context.annotation.aspectj;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SpringConfiguredConfiguration.class)
public @interface EnableSpringConfigured {

}
