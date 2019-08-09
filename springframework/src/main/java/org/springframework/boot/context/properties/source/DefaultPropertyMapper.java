package org.springframework.boot.context.properties.source;

import org.springframework.util.ObjectUtils;

final class DefaultPropertyMapper implements PropertyMapper {

    public static final PropertyMapper INSTANCE = new DefaultPropertyMapper();

    private LastMapping<ConfigurationPropertyName> lastMappedConfigurationPropertyName;

    private LastMapping<String> lastMappedPropertyName;

    private DefaultPropertyMapper() {
    }

    @Override
    public PropertyMapping[] map(ConfigurationPropertyName configurationPropertyName) {
        // Use a local copy in case another thread changes things
        LastMapping<ConfigurationPropertyName> last = this.lastMappedConfigurationPropertyName;
        if (last != null && last.isFrom(configurationPropertyName)) {
            return last.getMapping();
        }
        String convertedName = configurationPropertyName.toString();
        PropertyMapping[] mapping = {new PropertyMapping(convertedName, configurationPropertyName)};
        this.lastMappedConfigurationPropertyName = new LastMapping<>(configurationPropertyName, mapping);
        return mapping;
    }

    @Override
    public PropertyMapping[] map(String propertySourceName) {
        // Use a local copy in case another thread changes things
        LastMapping<String> last = this.lastMappedPropertyName;
        if (last != null && last.isFrom(propertySourceName)) {
            return last.getMapping();
        }
        PropertyMapping[] mapping = tryMap(propertySourceName);
        this.lastMappedPropertyName = new LastMapping<>(propertySourceName, mapping);
        return mapping;
    }

    private PropertyMapping[] tryMap(String propertySourceName) {
        try {
            ConfigurationPropertyName convertedName = ConfigurationPropertyName.adapt(propertySourceName, '.');
            if (!convertedName.isEmpty()) {
                return new PropertyMapping[]{new PropertyMapping(propertySourceName, convertedName)};
            }
        } catch (Exception ex) {
        }
        return NO_MAPPINGS;
    }

    private static class LastMapping<T> {

        private final T from;

        private final PropertyMapping[] mapping;

        LastMapping(T from, PropertyMapping[] mapping) {
            this.from = from;
            this.mapping = mapping;
        }

        public boolean isFrom(T from) {
            return ObjectUtils.nullSafeEquals(from, this.from);
        }

        public PropertyMapping[] getMapping() {
            return this.mapping;
        }

    }

}
