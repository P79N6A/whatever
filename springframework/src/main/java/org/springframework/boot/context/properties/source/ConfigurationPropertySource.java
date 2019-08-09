package org.springframework.boot.context.properties.source;

import java.util.function.Predicate;

@FunctionalInterface
public interface ConfigurationPropertySource {

    ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name);

    default ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
        return ConfigurationPropertyState.UNKNOWN;
    }

    default ConfigurationPropertySource filter(Predicate<ConfigurationPropertyName> filter) {
        return new FilteredConfigurationPropertiesSource(this, filter);
    }

    default ConfigurationPropertySource withAliases(ConfigurationPropertyNameAliases aliases) {
        return new AliasedConfigurationPropertySource(this, aliases);
    }

    default Object getUnderlyingSource() {
        return null;
    }

}
