package org.springframework.boot.context.properties.source;

import org.springframework.core.env.*;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.stream.Stream;

public final class ConfigurationPropertySources {

    private static final String ATTACHED_PROPERTY_SOURCE_NAME = "configurationProperties";

    private ConfigurationPropertySources() {
    }

    public static boolean isAttachedConfigurationPropertySource(PropertySource<?> propertySource) {
        return ATTACHED_PROPERTY_SOURCE_NAME.equals(propertySource.getName());
    }

    public static void attach(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        MutablePropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
        PropertySource<?> attached = sources.get(ATTACHED_PROPERTY_SOURCE_NAME);
        if (attached != null && attached.getSource() != sources) {
            sources.remove(ATTACHED_PROPERTY_SOURCE_NAME);
            attached = null;
        }
        if (attached == null) {
            sources.addFirst(new ConfigurationPropertySourcesPropertySource(ATTACHED_PROPERTY_SOURCE_NAME, new SpringConfigurationPropertySources(sources)));
        }
    }

    public static Iterable<ConfigurationPropertySource> get(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        MutablePropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
        ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) sources.get(ATTACHED_PROPERTY_SOURCE_NAME);
        if (attached == null) {
            return from(sources);
        }
        return attached.getSource();
    }

    public static Iterable<ConfigurationPropertySource> from(PropertySource<?> source) {
        return Collections.singleton(SpringConfigurationPropertySource.from(source));
    }

    public static Iterable<ConfigurationPropertySource> from(Iterable<PropertySource<?>> sources) {
        return new SpringConfigurationPropertySources(sources);
    }

    private static Stream<PropertySource<?>> streamPropertySources(PropertySources sources) {
        return sources.stream().flatMap(ConfigurationPropertySources::flatten).filter(ConfigurationPropertySources::isIncluded);
    }

    private static Stream<PropertySource<?>> flatten(PropertySource<?> source) {
        if (source.getSource() instanceof ConfigurableEnvironment) {
            return streamPropertySources(((ConfigurableEnvironment) source.getSource()).getPropertySources());
        }
        return Stream.of(source);
    }

    private static boolean isIncluded(PropertySource<?> source) {
        return !(source instanceof StubPropertySource) && !(source instanceof ConfigurationPropertySourcesPropertySource);
    }

}
