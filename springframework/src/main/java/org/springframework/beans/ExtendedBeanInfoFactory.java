package org.springframework.beans;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;

public class ExtendedBeanInfoFactory implements BeanInfoFactory, Ordered {

    @Override
    @Nullable
    public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        return (supports(beanClass) ? new ExtendedBeanInfo(Introspector.getBeanInfo(beanClass)) : null);
    }

    private boolean supports(Class<?> beanClass) {
        for (Method method : beanClass.getMethods()) {
            if (ExtendedBeanInfo.isCandidateWriteMethod(method)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
