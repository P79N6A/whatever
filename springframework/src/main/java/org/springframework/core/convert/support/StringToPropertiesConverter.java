package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class StringToPropertiesConverter implements Converter<String, Properties> {

    @Override
    public Properties convert(String source) {
        try {
            Properties props = new Properties();
            // Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
            props.load(new ByteArrayInputStream(source.getBytes(StandardCharsets.ISO_8859_1)));
            return props;
        } catch (Exception ex) {
            // Should never happen.
            throw new IllegalArgumentException("Failed to parse [" + source + "] into Properties", ex);
        }
    }

}
