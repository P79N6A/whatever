package org.springframework.web.servlet.handler;

import org.springframework.web.method.HandlerMethod;

@FunctionalInterface
public interface HandlerMethodMappingNamingStrategy<T> {

    String getName(HandlerMethod handlerMethod, T mapping);

}
