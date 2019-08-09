package org.springframework.beans.factory;

@SuppressWarnings("serial")
public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException {

    public BeanIsNotAFactoryException(String name, Class<?> actualType) {
        super(name, FactoryBean.class, actualType);
    }

}
