package org.springframework.core.convert.converter;

import org.springframework.lang.Nullable;

@FunctionalInterface
public interface Converter<S, T> {

    @Nullable
    T convert(S source);

}
