package org.springframework.boot.context.properties.source;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.PropertySource;

class ConfigurationPropertySourcesPropertySource extends PropertySource<Iterable<ConfigurationPropertySource>> implements OriginLookup<String> {

    ConfigurationPropertySourcesPropertySource(String name, Iterable<ConfigurationPropertySource> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        ConfigurationProperty configurationProperty = findConfigurationProperty(name);
        return (configurationProperty != null) ? configurationProperty.getValue() : null;
    }

    @Override
    public Origin getOrigin(String name) {
        return Origin.from(findConfigurationProperty(name));
    }

    private ConfigurationProperty findConfigurationProperty(String name) {
        try {
            return findConfigurationProperty(ConfigurationPropertyName.of(name, true));
        } catch (Exception ex) {
            return null;
        }
    }

    private ConfigurationProperty findConfigurationProperty(ConfigurationPropertyName name) {
        if (name == null) {
            return null;
        }
        for (ConfigurationPropertySource configurationPropertySource : getSource()) {
            ConfigurationProperty configurationProperty = configurationPropertySource.getConfigurationProperty(name);
            if (configurationProperty != null) {
                return configurationProperty;
            }
        }
        return null;
    }

}
