package org.springframework.boot.context.properties;

import org.springframework.util.Assert;

public class InvalidConfigurationPropertiesException extends RuntimeException {

    private final Class<?> configurationProperties;

    private final Class<?> component;

    public InvalidConfigurationPropertiesException(Class<?> configurationProperties, Class<?> component) {
        super("Found @" + component.getSimpleName() + " and @ConfigurationProperties on " + configurationProperties.getName() + ".");
        Assert.notNull(configurationProperties, "Class must not be null");
        this.configurationProperties = configurationProperties;
        this.component = component;
    }

    public Class<?> getConfigurationProperties() {
        return this.configurationProperties;
    }

    public Class<?> getComponent() {
        return this.component;
    }

}
