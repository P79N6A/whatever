package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

public interface BeanExpressionResolver {

    @Nullable
    Object evaluate(@Nullable String value, BeanExpressionContext evalContext) throws BeansException;

}
