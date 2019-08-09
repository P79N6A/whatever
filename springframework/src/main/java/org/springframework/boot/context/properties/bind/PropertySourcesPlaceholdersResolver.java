package org.springframework.boot.context.properties.bind;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

public class PropertySourcesPlaceholdersResolver implements PlaceholdersResolver {

    private final Iterable<PropertySource<?>> sources;

    private final PropertyPlaceholderHelper helper;

    public PropertySourcesPlaceholdersResolver(Environment environment) {
        this(getSources(environment), null);
    }

    public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources) {
        this(sources, null);
    }

    public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources, PropertyPlaceholderHelper helper) {
        this.sources = sources;
        this.helper = (helper != null) ? helper : new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX, SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);
    }

    @Override
    public Object resolvePlaceholders(Object value) {
        if (value != null && value instanceof String) {
            return this.helper.replacePlaceholders((String) value, this::resolvePlaceholder);
        }
        return value;
    }

    protected String resolvePlaceholder(String placeholder) {
        if (this.sources != null) {
            for (PropertySource<?> source : this.sources) {
                Object value = source.getProperty(placeholder);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }
        return null;
    }

    private static PropertySources getSources(Environment environment) {
        Assert.notNull(environment, "Environment must not be null");
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "Environment must be a ConfigurableEnvironment");
        return ((ConfigurableEnvironment) environment).getPropertySources();
    }

}
