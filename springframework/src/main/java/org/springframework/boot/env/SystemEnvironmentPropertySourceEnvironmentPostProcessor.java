package org.springframework.boot.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

public class SystemEnvironmentPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final int DEFAULT_ORDER = SpringApplicationJsonEnvironmentPostProcessor.DEFAULT_ORDER - 1;

    private int order = DEFAULT_ORDER;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String sourceName = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        PropertySource<?> propertySource = environment.getPropertySources().get(sourceName);
        if (propertySource != null) {
            replacePropertySource(environment, sourceName, propertySource);
        }
    }

    @SuppressWarnings("unchecked")
    private void replacePropertySource(ConfigurableEnvironment environment, String sourceName, PropertySource<?> propertySource) {
        Map<String, Object> originalSource = (Map<String, Object>) propertySource.getSource();
        SystemEnvironmentPropertySource source = new OriginAwareSystemEnvironmentPropertySource(sourceName, originalSource);
        environment.getPropertySources().replace(sourceName, source);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    protected static class OriginAwareSystemEnvironmentPropertySource extends SystemEnvironmentPropertySource implements OriginLookup<String> {

        OriginAwareSystemEnvironmentPropertySource(String name, Map<String, Object> source) {
            super(name, source);
        }

        @Override
        public Origin getOrigin(String key) {
            String property = resolvePropertyName(key);
            if (super.containsProperty(property)) {
                return new SystemEnvironmentOrigin(property);
            }
            return null;
        }

    }

}
