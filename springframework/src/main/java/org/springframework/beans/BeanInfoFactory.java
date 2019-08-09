package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

public interface BeanInfoFactory {

    @Nullable
    BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}
