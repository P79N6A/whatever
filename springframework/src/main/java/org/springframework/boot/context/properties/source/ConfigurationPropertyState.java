package org.springframework.boot.context.properties.source;

import org.springframework.util.Assert;

import java.util.function.Predicate;

public enum ConfigurationPropertyState {

    PRESENT,

    ABSENT,

    UNKNOWN;

    static <T> ConfigurationPropertyState search(Iterable<T> source, Predicate<T> predicate) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(predicate, "Predicate must not be null");
        for (T item : source) {
            if (predicate.test(item)) {
                return PRESENT;
            }
        }
        return ABSENT;
    }

}
