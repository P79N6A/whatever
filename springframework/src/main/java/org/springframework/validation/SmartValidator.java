package org.springframework.validation;

import org.springframework.lang.Nullable;

public interface SmartValidator extends Validator {

    void validate(Object target, Errors errors, Object... validationHints);

    default void validateValue(Class<?> targetType, String fieldName, @Nullable Object value, Errors errors, Object... validationHints) {
        throw new IllegalArgumentException("Cannot validate individual value for " + targetType);
    }

}
