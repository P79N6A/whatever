package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;

public class InstanceFilter<T> {

    private final Collection<? extends T> includes;

    private final Collection<? extends T> excludes;

    private final boolean matchIfEmpty;

    public InstanceFilter(@Nullable Collection<? extends T> includes, @Nullable Collection<? extends T> excludes, boolean matchIfEmpty) {
        this.includes = (includes != null ? includes : Collections.emptyList());
        this.excludes = (excludes != null ? excludes : Collections.emptyList());
        this.matchIfEmpty = matchIfEmpty;
    }

    public boolean match(T instance) {
        Assert.notNull(instance, "Instance to match must not be null");
        boolean includesSet = !this.includes.isEmpty();
        boolean excludesSet = !this.excludes.isEmpty();
        if (!includesSet && !excludesSet) {
            return this.matchIfEmpty;
        }
        boolean matchIncludes = match(instance, this.includes);
        boolean matchExcludes = match(instance, this.excludes);
        if (!includesSet) {
            return !matchExcludes;
        }
        if (!excludesSet) {
            return matchIncludes;
        }
        return matchIncludes && !matchExcludes;
    }

    protected boolean match(T instance, T candidate) {
        return instance.equals(candidate);
    }

    protected boolean match(T instance, Collection<? extends T> candidates) {
        for (T candidate : candidates) {
            if (match(instance, candidate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(": includes=").append(this.includes);
        sb.append(", excludes=").append(this.excludes);
        sb.append(", matchIfEmpty=").append(this.matchIfEmpty);
        return sb.toString();
    }

}
