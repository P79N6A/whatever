package org.springframework.context.event;

import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

public class DefaultEventListenerFactory implements EventListenerFactory, Ordered {

    private int order = LOWEST_PRECEDENCE;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public boolean supportsMethod(Method method) {
        return true;
    }

    @Override
    public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
        return new ApplicationListenerMethodAdapter(beanName, type, method);
    }

}
