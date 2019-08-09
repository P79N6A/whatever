package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

@FunctionalInterface
interface AggregateElementBinder {

    default Object bind(ConfigurationPropertyName name, Bindable<?> target) {
        return bind(name, target, null);
    }

    Object bind(ConfigurationPropertyName name, Bindable<?> target, ConfigurationPropertySource source);

}
