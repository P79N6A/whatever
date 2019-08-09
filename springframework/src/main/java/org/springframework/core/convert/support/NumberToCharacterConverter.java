package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

final class NumberToCharacterConverter implements Converter<Number, Character> {

    @Override
    public Character convert(Number source) {
        return (char) source.shortValue();
    }

}
