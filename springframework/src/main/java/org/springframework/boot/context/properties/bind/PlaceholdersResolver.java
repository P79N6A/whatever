package org.springframework.boot.context.properties.bind;

@FunctionalInterface
public interface PlaceholdersResolver {

    PlaceholdersResolver NONE = (value) -> value;

    Object resolvePlaceholders(Object value);

}
