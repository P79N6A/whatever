package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

import java.util.Currency;

class StringToCurrencyConverter implements Converter<String, Currency> {

    @Override
    public Currency convert(String source) {
        return Currency.getInstance(source);
    }

}
