package org.springframework.beans;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;

public interface TypeConverter {

    @Nullable
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

    @Nullable
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable MethodParameter methodParam) throws TypeMismatchException;

    @Nullable
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field) throws TypeMismatchException;

    @Nullable
    default <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {
        throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
    }

}
