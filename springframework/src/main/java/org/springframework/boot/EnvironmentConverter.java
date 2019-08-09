package org.springframework.boot;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class EnvironmentConverter {

    private static final String CONFIGURABLE_WEB_ENVIRONMENT_CLASS = "org.springframework.web.context.ConfigurableWebEnvironment";

    private static final Set<String> SERVLET_ENVIRONMENT_SOURCE_NAMES;

    static {
        Set<String> names = new HashSet<>();
        names.add(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME);
        names.add(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME);
        names.add(StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME);
        SERVLET_ENVIRONMENT_SOURCE_NAMES = Collections.unmodifiableSet(names);
    }

    private final ClassLoader classLoader;

    EnvironmentConverter(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    StandardEnvironment convertEnvironmentIfNecessary(ConfigurableEnvironment environment, Class<? extends StandardEnvironment> type) {
        if (type.equals(environment.getClass())) {
            return (StandardEnvironment) environment;
        }
        return convertEnvironment(environment, type);
    }

    private StandardEnvironment convertEnvironment(ConfigurableEnvironment environment, Class<? extends StandardEnvironment> type) {
        StandardEnvironment result = createEnvironment(type);
        result.setActiveProfiles(environment.getActiveProfiles());
        result.setConversionService(environment.getConversionService());
        copyPropertySources(environment, result);
        return result;
    }

    private StandardEnvironment createEnvironment(Class<? extends StandardEnvironment> type) {
        try {
            return type.newInstance();
        } catch (Exception ex) {
            return new StandardEnvironment();
        }
    }

    private void copyPropertySources(ConfigurableEnvironment source, StandardEnvironment target) {
        removePropertySources(target.getPropertySources(), isServletEnvironment(target.getClass(), this.classLoader));
        for (PropertySource<?> propertySource : source.getPropertySources()) {
            if (!SERVLET_ENVIRONMENT_SOURCE_NAMES.contains(propertySource.getName())) {
                target.getPropertySources().addLast(propertySource);
            }
        }
    }

    private boolean isServletEnvironment(Class<?> conversionType, ClassLoader classLoader) {
        try {
            Class<?> webEnvironmentClass = ClassUtils.forName(CONFIGURABLE_WEB_ENVIRONMENT_CLASS, classLoader);
            return webEnvironmentClass.isAssignableFrom(conversionType);
        } catch (Throwable ex) {
            return false;
        }
    }

    private void removePropertySources(MutablePropertySources propertySources, boolean isServletEnvironment) {
        Set<String> names = new HashSet<>();
        for (PropertySource<?> propertySource : propertySources) {
            names.add(propertySource.getName());
        }
        for (String name : names) {
            if (!isServletEnvironment || !SERVLET_ENVIRONMENT_SOURCE_NAMES.contains(name)) {
                propertySources.remove(name);
            }
        }
    }

}
