package org.springframework.context.event;

import org.springframework.context.ApplicationListener;

import java.lang.reflect.Method;

public interface EventListenerFactory {

    boolean supportsMethod(Method method);

    ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method);

}
