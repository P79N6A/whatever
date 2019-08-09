package org.springframework.boot.autoconfigure.template;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import java.util.List;

public abstract class PathBasedTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

    private final String className;

    private final Class<TemplateAvailabilityProperties> propertiesClass;

    private final String propertyPrefix;

    @SuppressWarnings("unchecked")
    public PathBasedTemplateAvailabilityProvider(String className, Class<? extends TemplateAvailabilityProperties> propertiesClass, String propertyPrefix) {
        this.className = className;
        this.propertiesClass = (Class<TemplateAvailabilityProperties>) propertiesClass;
        this.propertyPrefix = propertyPrefix;
    }

    @Override
    public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader, ResourceLoader resourceLoader) {
        if (ClassUtils.isPresent(this.className, classLoader)) {
            Binder binder = Binder.get(environment);
            TemplateAvailabilityProperties properties = binder.bind(this.propertyPrefix, this.propertiesClass).orElseCreate(this.propertiesClass);
            return isTemplateAvailable(view, resourceLoader, properties);
        }
        return false;
    }

    private boolean isTemplateAvailable(String view, ResourceLoader resourceLoader, TemplateAvailabilityProperties properties) {
        String location = properties.getPrefix() + view + properties.getSuffix();
        for (String path : properties.getLoaderPath()) {
            if (resourceLoader.getResource(path + location).exists()) {
                return true;
            }
        }
        return false;
    }

    protected abstract static class TemplateAvailabilityProperties {

        private String prefix;

        private String suffix;

        protected TemplateAvailabilityProperties(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        protected abstract List<String> getLoaderPath();

        public String getPrefix() {
            return this.prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getSuffix() {
            return this.suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

    }

}
