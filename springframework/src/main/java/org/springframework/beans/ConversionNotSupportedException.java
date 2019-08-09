package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.beans.PropertyChangeEvent;

@SuppressWarnings("serial")
public class ConversionNotSupportedException extends TypeMismatchException {

    public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
        super(propertyChangeEvent, requiredType, cause);
    }

    public ConversionNotSupportedException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
        super(value, requiredType, cause);
    }

}
