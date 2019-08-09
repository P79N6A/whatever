package org.springframework.core.convert.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;

final class FallbackObjectToStringConverter implements ConditionalGenericConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, String.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        Class<?> sourceClass = sourceType.getObjectType();
        if (String.class == sourceClass) {
            // no conversion required
            return false;
        }
        return (CharSequence.class.isAssignableFrom(sourceClass) || StringWriter.class.isAssignableFrom(sourceClass) || ObjectToObjectConverter.hasConversionMethodOrConstructor(sourceClass, String.class));
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return (source != null ? source.toString() : null);
    }

}
