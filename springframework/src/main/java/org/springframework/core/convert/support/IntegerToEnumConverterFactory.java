package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
final class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum> {

    @Override
    public <T extends Enum> Converter<Integer, T> getConverter(Class<T> targetType) {
        return new IntegerToEnum(ConversionUtils.getEnumType(targetType));
    }

    private static class IntegerToEnum<T extends Enum> implements Converter<Integer, T> {

        private final Class<T> enumType;

        public IntegerToEnum(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(Integer source) {
            return this.enumType.getEnumConstants()[source];
        }

    }

}
