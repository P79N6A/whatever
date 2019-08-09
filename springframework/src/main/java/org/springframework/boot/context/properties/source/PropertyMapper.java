package org.springframework.boot.context.properties.source;

interface PropertyMapper {

    PropertyMapping[] NO_MAPPINGS = {};

    PropertyMapping[] map(ConfigurationPropertyName configurationPropertyName);

    PropertyMapping[] map(String propertySourceName);

}
