package org.springframework.web.method.annotation;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class MethodArgumentTypeMismatchException extends TypeMismatchException {

    private final String name;

    private final MethodParameter parameter;

    public MethodArgumentTypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType, String name, MethodParameter param, Throwable cause) {
        super(value, requiredType, cause);
        this.name = name;
        this.parameter = param;
    }

    public String getName() {
        return this.name;
    }

    public MethodParameter getParameter() {
        return this.parameter;
    }

}
