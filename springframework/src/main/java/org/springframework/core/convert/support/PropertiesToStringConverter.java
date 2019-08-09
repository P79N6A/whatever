package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

final class PropertiesToStringConverter implements Converter<Properties, String> {

    @Override
    public String convert(Properties source) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(256);
            source.store(os, null);
            return os.toString("ISO-8859-1");
        } catch (IOException ex) {
            // Should never happen.
            throw new IllegalArgumentException("Failed to store [" + source + "] into String", ex);
        }
    }

}
