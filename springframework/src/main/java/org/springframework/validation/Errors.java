package org.springframework.validation;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

import java.util.List;

public interface Errors {

    String NESTED_PATH_SEPARATOR = PropertyAccessor.NESTED_PROPERTY_SEPARATOR;

    String getObjectName();

    void setNestedPath(String nestedPath);

    String getNestedPath();

    void pushNestedPath(String subPath);

    void popNestedPath() throws IllegalStateException;

    void reject(String errorCode);

    void reject(String errorCode, String defaultMessage);

    void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage);

    void rejectValue(@Nullable String field, String errorCode);

    void rejectValue(@Nullable String field, String errorCode, String defaultMessage);

    void rejectValue(@Nullable String field, String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage);

    void addAllErrors(Errors errors);

    boolean hasErrors();

    int getErrorCount();

    List<ObjectError> getAllErrors();

    boolean hasGlobalErrors();

    int getGlobalErrorCount();

    List<ObjectError> getGlobalErrors();

    @Nullable
    ObjectError getGlobalError();

    boolean hasFieldErrors();

    int getFieldErrorCount();

    List<FieldError> getFieldErrors();

    @Nullable
    FieldError getFieldError();

    boolean hasFieldErrors(String field);

    int getFieldErrorCount(String field);

    List<FieldError> getFieldErrors(String field);

    @Nullable
    FieldError getFieldError(String field);

    @Nullable
    Object getFieldValue(String field);

    @Nullable
    Class<?> getFieldType(String field);

}
