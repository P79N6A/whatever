package org.springframework.boot.origin;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public class SystemEnvironmentOrigin implements Origin {

    private final String property;

    public SystemEnvironmentOrigin(String property) {
        Assert.notNull(property, "Property name must not be null");
        Assert.hasText(property, "Property name must not be empty");
        this.property = property;
    }

    public String getProperty() {
        return this.property;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SystemEnvironmentOrigin other = (SystemEnvironmentOrigin) obj;
        return ObjectUtils.nullSafeEquals(this.property, other.property);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.property);
    }

    @Override
    public String toString() {
        return "System Environment Property \"" + this.property + "\"";
    }

}
