package org.springframework.validation.beanvalidation;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

public class SpringConstraintValidatorFactory implements ConstraintValidatorFactory {

    private final AutowireCapableBeanFactory beanFactory;

    public SpringConstraintValidatorFactory(AutowireCapableBeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
    }

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        return this.beanFactory.createBean(key);
    }

    // Bean Validation 1.1 releaseInstance method
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        this.beanFactory.destroyBean(instance);
    }

}
