package org.springframework.boot.convert;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

final class NumberToDurationConverter implements GenericConverter {

    private final StringToDurationConverter delegate = new StringToDurationConverter();

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Number.class, Duration.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.delegate.convert((source != null) ? source.toString() : null, TypeDescriptor.valueOf(String.class), targetType);
    }

}
