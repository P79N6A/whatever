package org.springframework.aop.aspectj.annotation;

import org.springframework.beans.factory.BeanFactory;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PrototypeAspectInstanceFactory extends BeanFactoryAspectInstanceFactory implements Serializable {

    public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
        super(beanFactory, name);
        if (!beanFactory.isPrototype(name)) {
            throw new IllegalArgumentException("Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
        }
    }

}
