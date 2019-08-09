package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface PropertyValues extends Iterable<PropertyValue> {

    @Override
    default Iterator<PropertyValue> iterator() {
        return Arrays.asList(getPropertyValues()).iterator();
    }

    @Override
    default Spliterator<PropertyValue> spliterator() {
        return Spliterators.spliterator(getPropertyValues(), 0);
    }

    default Stream<PropertyValue> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    PropertyValue[] getPropertyValues();

    @Nullable
    PropertyValue getPropertyValue(String propertyName);

    PropertyValues changesSince(PropertyValues old);

    boolean contains(String propertyName);

    boolean isEmpty();

}
