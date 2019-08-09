package org.springframework.beans;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Field;

public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {

    @Nullable
    TypeConverterDelegate typeConverterDelegate;

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
        return convertIfNecessary(value, requiredType, TypeDescriptor.valueOf(requiredType));
    }

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable MethodParameter methodParam) throws TypeMismatchException {
        return convertIfNecessary(value, requiredType, (methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType)));
    }

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field) throws TypeMismatchException {
        return convertIfNecessary(value, requiredType, (field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType)));
    }

    @Nullable
    @Override
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {
        Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
        try {
            return this.typeConverterDelegate.convertIfNecessary(null, null, value, requiredType, typeDescriptor);
        } catch (ConverterNotFoundException | IllegalStateException ex) {
            throw new ConversionNotSupportedException(value, requiredType, ex);
        } catch (ConversionException | IllegalArgumentException ex) {
            throw new TypeMismatchException(value, requiredType, ex);
        }
    }

}
