package org.springframework.core.annotation;

import org.springframework.core.NestedRuntimeException;

@SuppressWarnings("serial")
public class AnnotationConfigurationException extends NestedRuntimeException {

    public AnnotationConfigurationException(String message) {
        super(message);
    }

    public AnnotationConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
