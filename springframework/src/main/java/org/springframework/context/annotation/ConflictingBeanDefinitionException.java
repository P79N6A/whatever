package org.springframework.context.annotation;

@SuppressWarnings("serial")
class ConflictingBeanDefinitionException extends IllegalStateException {

    public ConflictingBeanDefinitionException(String message) {
        super(message);
    }

}
