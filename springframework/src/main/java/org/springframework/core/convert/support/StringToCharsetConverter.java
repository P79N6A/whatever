package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

import java.nio.charset.Charset;

class StringToCharsetConverter implements Converter<String, Charset> {

    @Override
    public Charset convert(String source) {
        return Charset.forName(source);
    }

}
