package org.springframework.core.convert;

import org.springframework.core.NestedRuntimeException;

@SuppressWarnings("serial")
public abstract class ConversionException extends NestedRuntimeException {

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

}
