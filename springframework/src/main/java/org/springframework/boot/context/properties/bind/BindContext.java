package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

public interface BindContext {

    Binder getBinder();

    int getDepth();

    Iterable<ConfigurationPropertySource> getSources();

    ConfigurationProperty getConfigurationProperty();

}
