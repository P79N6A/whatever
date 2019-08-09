package org.springframework.validation.beanvalidation;

import org.apache.commons.logging.LogFactory;

import javax.validation.ValidationException;

public class OptionalValidatorFactoryBean extends LocalValidatorFactoryBean {

    @Override
    public void afterPropertiesSet() {
        try {
            super.afterPropertiesSet();
        } catch (ValidationException ex) {
            LogFactory.getLog(getClass()).debug("Failed to set up a Bean Validation provider", ex);
        }
    }

}
