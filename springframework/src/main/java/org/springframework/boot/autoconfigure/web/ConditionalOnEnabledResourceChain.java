package org.springframework.boot.autoconfigure.web;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnEnabledResourceChainCondition.class)
public @interface ConditionalOnEnabledResourceChain {

}
