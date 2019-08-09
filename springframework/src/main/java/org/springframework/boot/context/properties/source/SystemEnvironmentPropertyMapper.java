package org.springframework.boot.context.properties.source;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;

import java.util.Locale;

final class SystemEnvironmentPropertyMapper implements PropertyMapper {

    public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();

    @Override
    public PropertyMapping[] map(ConfigurationPropertyName configurationPropertyName) {
        String name = convertName(configurationPropertyName);
        String legacyName = convertLegacyName(configurationPropertyName);
        if (name.equals(legacyName)) {
            return new PropertyMapping[]{new PropertyMapping(name, configurationPropertyName)};
        }
        return new PropertyMapping[]{new PropertyMapping(name, configurationPropertyName), new PropertyMapping(legacyName, configurationPropertyName)};
    }

    @Override
    public PropertyMapping[] map(String propertySourceName) {
        ConfigurationPropertyName name = convertName(propertySourceName);
        if (name == null || name.isEmpty()) {
            return NO_MAPPINGS;
        }
        return new PropertyMapping[]{new PropertyMapping(propertySourceName, name)};
    }

    private ConfigurationPropertyName convertName(String propertySourceName) {
        try {
            return ConfigurationPropertyName.adapt(propertySourceName, '_', this::processElementValue);
        } catch (Exception ex) {
            return null;
        }
    }

    private String convertName(ConfigurationPropertyName name) {
        return convertName(name, name.getNumberOfElements());
    }

    private String convertName(ConfigurationPropertyName name, int numberOfElements) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfElements; i++) {
            if (result.length() > 0) {
                result.append("_");
            }
            result.append(name.getElement(i, Form.UNIFORM).toUpperCase(Locale.ENGLISH));
        }
        return result.toString();
    }

    private String convertLegacyName(ConfigurationPropertyName name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.getNumberOfElements(); i++) {
            if (result.length() > 0) {
                result.append("_");
            }
            result.append(convertLegacyNameElement(name.getElement(i, Form.ORIGINAL)));
        }
        return result.toString();
    }

    private Object convertLegacyNameElement(String element) {
        return element.replace('-', '_').toUpperCase(Locale.ENGLISH);
    }

    private CharSequence processElementValue(CharSequence value) {
        String result = value.toString().toLowerCase(Locale.ENGLISH);
        return isNumber(result) ? "[" + result + "]" : result;
    }

    private static boolean isNumber(String string) {
        return string.chars().allMatch(Character::isDigit);
    }

}
