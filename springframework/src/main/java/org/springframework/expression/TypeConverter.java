package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

public interface TypeConverter {

    boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

    @Nullable
    Object convertValue(@Nullable Object value, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
