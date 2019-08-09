package org.springframework.boot.logging;

import org.springframework.core.env.PropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Properties;

public class LogFile {

    @Deprecated
    public static final String FILE_PROPERTY = "logging.file";

    @Deprecated
    public static final String PATH_PROPERTY = "logging.path";

    public static final String FILE_NAME_PROPERTY = "logging.file.name";

    public static final String FILE_PATH_PROPERTY = "logging.file.path";

    private final String file;

    private final String path;

    LogFile(String file) {
        this(file, null);
    }

    LogFile(String file, String path) {
        Assert.isTrue(StringUtils.hasLength(file) || StringUtils.hasLength(path), "File or Path must not be empty");
        this.file = file;
        this.path = path;
    }

    public void applyToSystemProperties() {
        applyTo(System.getProperties());
    }

    public void applyTo(Properties properties) {
        put(properties, LoggingSystemProperties.LOG_PATH, this.path);
        put(properties, LoggingSystemProperties.LOG_FILE, toString());
    }

    private void put(Properties properties, String key, String value) {
        if (StringUtils.hasLength(value)) {
            properties.put(key, value);
        }
    }

    @Override
    public String toString() {
        if (StringUtils.hasLength(this.file)) {
            return this.file;
        }
        return new File(this.path, "spring.log").getPath();
    }

    public static LogFile get(PropertyResolver propertyResolver) {
        String file = getLogFileProperty(propertyResolver, FILE_NAME_PROPERTY, FILE_PROPERTY);
        String path = getLogFileProperty(propertyResolver, FILE_PATH_PROPERTY, PATH_PROPERTY);
        if (StringUtils.hasLength(file) || StringUtils.hasLength(path)) {
            return new LogFile(file, path);
        }
        return null;
    }

    private static String getLogFileProperty(PropertyResolver propertyResolver, String propertyName, String deprecatedPropertyName) {
        String property = propertyResolver.getProperty(propertyName);
        if (property != null) {
            return property;
        }
        return propertyResolver.getProperty(deprecatedPropertyName);
    }

}
