package org.springframework.web.bind.support;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

public class SpringWebConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        return getWebApplicationContext().getAutowireCapableBeanFactory().createBean(key);
    }

    // Bean Validation 1.1 releaseInstance method
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        getWebApplicationContext().getAutowireCapableBeanFactory().destroyBean(instance);
    }

    protected WebApplicationContext getWebApplicationContext() {
        WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
        if (wac == null) {
            throw new IllegalStateException("No WebApplicationContext registered for current thread - " + "consider overriding SpringWebConstraintValidatorFactory.getWebApplicationContext()");
        }
        return wac;
    }

}
