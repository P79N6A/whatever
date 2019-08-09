package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Locale;

final class StringToLocaleConverter implements Converter<String, Locale> {

    @Override
    @Nullable
    public Locale convert(String source) {
        return StringUtils.parseLocale(source);
    }

}
