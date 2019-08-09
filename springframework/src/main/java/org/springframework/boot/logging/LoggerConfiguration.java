package org.springframework.boot.logging;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public final class LoggerConfiguration {

    private final String name;

    private final LogLevel configuredLevel;

    private final LogLevel effectiveLevel;

    public LoggerConfiguration(String name, LogLevel configuredLevel, LogLevel effectiveLevel) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(effectiveLevel, "EffectiveLevel must not be null");
        this.name = name;
        this.configuredLevel = configuredLevel;
        this.effectiveLevel = effectiveLevel;
    }

    public LogLevel getConfiguredLevel() {
        return this.configuredLevel;
    }

    public LogLevel getEffectiveLevel() {
        return this.effectiveLevel;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof LoggerConfiguration) {
            LoggerConfiguration other = (LoggerConfiguration) obj;
            boolean rtn = true;
            rtn = rtn && ObjectUtils.nullSafeEquals(this.name, other.name);
            rtn = rtn && ObjectUtils.nullSafeEquals(this.configuredLevel, other.configuredLevel);
            rtn = rtn && ObjectUtils.nullSafeEquals(this.effectiveLevel, other.effectiveLevel);
            return rtn;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
        result = prime * result + ObjectUtils.nullSafeHashCode(this.configuredLevel);
        result = prime * result + ObjectUtils.nullSafeHashCode(this.effectiveLevel);
        return result;
    }

    @Override
    public String toString() {
        return "LoggerConfiguration [name=" + this.name + ", configuredLevel=" + this.configuredLevel + ", effectiveLevel=" + this.effectiveLevel + "]";
    }

}
