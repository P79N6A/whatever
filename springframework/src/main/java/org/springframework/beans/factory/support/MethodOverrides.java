package org.springframework.beans.factory.support;

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MethodOverrides {

    private final Set<MethodOverride> overrides = Collections.synchronizedSet(new LinkedHashSet<>(2));

    private volatile boolean modified = false;

    public MethodOverrides() {
    }

    public MethodOverrides(MethodOverrides other) {
        addOverrides(other);
    }

    public void addOverrides(@Nullable MethodOverrides other) {
        if (other != null) {
            this.modified = true;
            this.overrides.addAll(other.overrides);
        }
    }

    public void addOverride(MethodOverride override) {
        this.modified = true;
        this.overrides.add(override);
    }

    public Set<MethodOverride> getOverrides() {
        this.modified = true;
        return this.overrides;
    }

    public boolean isEmpty() {
        return (!this.modified || this.overrides.isEmpty());
    }

    @Nullable
    public MethodOverride getOverride(Method method) {
        if (!this.modified) {
            return null;
        }
        synchronized (this.overrides) {
            MethodOverride match = null;
            for (MethodOverride candidate : this.overrides) {
                if (candidate.matches(method)) {
                    match = candidate;
                }
            }
            return match;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodOverrides)) {
            return false;
        }
        MethodOverrides that = (MethodOverrides) other;
        return this.overrides.equals(that.overrides);

    }

    @Override
    public int hashCode() {
        return this.overrides.hashCode();
    }

}
