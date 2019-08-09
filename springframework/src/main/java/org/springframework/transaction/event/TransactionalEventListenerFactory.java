package org.springframework.transaction.event;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

public class TransactionalEventListenerFactory implements EventListenerFactory, Ordered {

    private int order = 50;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public boolean supportsMethod(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class);
    }

    @Override
    public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
        return new ApplicationListenerMethodTransactionalAdapter(beanName, type, method);
    }

}
