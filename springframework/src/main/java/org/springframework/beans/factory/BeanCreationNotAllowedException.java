package org.springframework.beans.factory;

@SuppressWarnings("serial")
public class BeanCreationNotAllowedException extends BeanCreationException {

    public BeanCreationNotAllowedException(String beanName, String msg) {
        super(beanName, msg);
    }

}
