package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.*;

public class CompositePropertySource extends EnumerablePropertySource<Object> {

    private final Set<PropertySource<?>> propertySources = new LinkedHashSet<>();

    public CompositePropertySource(String name) {
        super(name);
    }

    @Override
    @Nullable
    public Object getProperty(String name) {
        for (PropertySource<?> propertySource : this.propertySources) {
            Object candidate = propertySource.getProperty(name);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean containsProperty(String name) {
        for (PropertySource<?> propertySource : this.propertySources) {
            if (propertySource.containsProperty(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> names = new LinkedHashSet<>();
        for (PropertySource<?> propertySource : this.propertySources) {
            if (!(propertySource instanceof EnumerablePropertySource)) {
                throw new IllegalStateException("Failed to enumerate property names due to non-enumerable property source: " + propertySource);
            }
            names.addAll(Arrays.asList(((EnumerablePropertySource<?>) propertySource).getPropertyNames()));
        }
        return StringUtils.toStringArray(names);
    }

    public void addPropertySource(PropertySource<?> propertySource) {
        this.propertySources.add(propertySource);
    }

    public void addFirstPropertySource(PropertySource<?> propertySource) {
        List<PropertySource<?>> existing = new ArrayList<>(this.propertySources);
        this.propertySources.clear();
        this.propertySources.add(propertySource);
        this.propertySources.addAll(existing);
    }

    public Collection<PropertySource<?>> getPropertySources() {
        return this.propertySources;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name='" + this.name + "', propertySources=" + this.propertySources + "}";
    }

}
