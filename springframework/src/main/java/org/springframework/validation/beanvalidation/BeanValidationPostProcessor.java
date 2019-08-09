package org.springframework.validation.beanvalidation;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Iterator;
import java.util.Set;

public class BeanValidationPostProcessor implements BeanPostProcessor, InitializingBean {

    @Nullable
    private Validator validator;

    private boolean afterInitialization = false;

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public void setValidatorFactory(ValidatorFactory validatorFactory) {
        this.validator = validatorFactory.getValidator();
    }

    public void setAfterInitialization(boolean afterInitialization) {
        this.afterInitialization = afterInitialization;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.validator == null) {
            this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!this.afterInitialization) {
            doValidate(bean);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (this.afterInitialization) {
            doValidate(bean);
        }
        return bean;
    }

    protected void doValidate(Object bean) {
        Assert.state(this.validator != null, "No Validator set");
        Object objectToValidate = AopProxyUtils.getSingletonTarget(bean);
        if (objectToValidate == null) {
            objectToValidate = bean;
        }
        Set<ConstraintViolation<Object>> result = this.validator.validate(objectToValidate);
        if (!result.isEmpty()) {
            StringBuilder sb = new StringBuilder("Bean state is invalid: ");
            for (Iterator<ConstraintViolation<Object>> it = result.iterator(); it.hasNext(); ) {
                ConstraintViolation<Object> violation = it.next();
                sb.append(violation.getPropertyPath()).append(" - ").append(violation.getMessage());
                if (it.hasNext()) {
                    sb.append("; ");
                }
            }
            throw new BeanInitializationException(sb.toString());
        }
    }

}
