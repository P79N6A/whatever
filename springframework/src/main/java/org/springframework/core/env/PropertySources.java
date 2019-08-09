package org.springframework.core.env;

import org.springframework.lang.Nullable;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface PropertySources extends Iterable<PropertySource<?>> {

    default Stream<PropertySource<?>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    boolean contains(String name);

    @Nullable
    PropertySource<?> get(String name);

}
