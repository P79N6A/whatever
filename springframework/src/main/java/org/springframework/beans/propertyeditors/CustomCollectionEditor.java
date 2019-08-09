package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Array;
import java.util.*;

public class CustomCollectionEditor extends PropertyEditorSupport {

    @SuppressWarnings("rawtypes")
    private final Class<? extends Collection> collectionType;

    private final boolean nullAsEmptyCollection;

    @SuppressWarnings("rawtypes")
    public CustomCollectionEditor(Class<? extends Collection> collectionType) {
        this(collectionType, false);
    }

    @SuppressWarnings("rawtypes")
    public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection) {
        Assert.notNull(collectionType, "Collection type is required");
        if (!Collection.class.isAssignableFrom(collectionType)) {
            throw new IllegalArgumentException("Collection type [" + collectionType.getName() + "] does not implement [java.util.Collection]");
        }
        this.collectionType = collectionType;
        this.nullAsEmptyCollection = nullAsEmptyCollection;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(text);
    }

    @Override
    public void setValue(@Nullable Object value) {
        if (value == null && this.nullAsEmptyCollection) {
            super.setValue(createCollection(this.collectionType, 0));
        } else if (value == null || (this.collectionType.isInstance(value) && !alwaysCreateNewCollection())) {
            // Use the source value as-is, as it matches the target type.
            super.setValue(value);
        } else if (value instanceof Collection) {
            // Convert Collection elements.
            Collection<?> source = (Collection<?>) value;
            Collection<Object> target = createCollection(this.collectionType, source.size());
            for (Object elem : source) {
                target.add(convertElement(elem));
            }
            super.setValue(target);
        } else if (value.getClass().isArray()) {
            // Convert array elements to Collection elements.
            int length = Array.getLength(value);
            Collection<Object> target = createCollection(this.collectionType, length);
            for (int i = 0; i < length; i++) {
                target.add(convertElement(Array.get(value, i)));
            }
            super.setValue(target);
        } else {
            // A plain value: convert it to a Collection with a single element.
            Collection<Object> target = createCollection(this.collectionType, 1);
            target.add(convertElement(value));
            super.setValue(target);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
        if (!collectionType.isInterface()) {
            try {
                return ReflectionUtils.accessibleConstructor(collectionType).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate collection class: " + collectionType.getName(), ex);
            }
        } else if (List.class == collectionType) {
            return new ArrayList<>(initialCapacity);
        } else if (SortedSet.class == collectionType) {
            return new TreeSet<>();
        } else {
            return new LinkedHashSet<>(initialCapacity);
        }
    }

    protected boolean alwaysCreateNewCollection() {
        return false;
    }

    protected Object convertElement(Object element) {
        return element;
    }

    @Override
    @Nullable
    public String getAsText() {
        return null;
    }

}
