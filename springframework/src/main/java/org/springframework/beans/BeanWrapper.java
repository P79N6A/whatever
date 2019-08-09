package org.springframework.beans;

import java.beans.PropertyDescriptor;

public interface BeanWrapper extends ConfigurablePropertyAccessor {

    void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

    int getAutoGrowCollectionLimit();

    Object getWrappedInstance();

    Class<?> getWrappedClass();

    PropertyDescriptor[] getPropertyDescriptors();

    PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
