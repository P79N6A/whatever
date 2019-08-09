package org.springframework.boot.context.properties.source;

import org.springframework.util.Assert;

@SuppressWarnings("serial")
public class InvalidConfigurationPropertyValueException extends RuntimeException {

    private final String name;

    private final Object value;

    private final String reason;

    public InvalidConfigurationPropertyValueException(String name, Object value, String reason) {
        super("Property " + name + " with value '" + value + "' is invalid: " + reason);
        Assert.notNull(name, "Name must not be null");
        this.name = name;
        this.value = value;
        this.reason = reason;
    }

    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return this.value;
    }

    public String getReason() {
        return this.reason;
    }

}
