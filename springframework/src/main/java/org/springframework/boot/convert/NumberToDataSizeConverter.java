package org.springframework.boot.convert;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.unit.DataSize;

import java.util.Collections;
import java.util.Set;

final class NumberToDataSizeConverter implements GenericConverter {

    private final StringToDataSizeConverter delegate = new StringToDataSizeConverter();

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Number.class, DataSize.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.delegate.convert((source != null) ? source.toString() : null, TypeDescriptor.valueOf(String.class), targetType);
    }

}
