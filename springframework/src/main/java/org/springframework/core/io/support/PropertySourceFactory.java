package org.springframework.core.io.support;

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

import java.io.IOException;

public interface PropertySourceFactory {

    PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
