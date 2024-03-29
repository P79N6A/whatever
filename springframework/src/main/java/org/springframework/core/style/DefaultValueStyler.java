package org.springframework.core.style;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.*;

public class DefaultValueStyler implements ValueStyler {

    private static final String EMPTY = "[empty]";

    private static final String NULL = "[null]";

    private static final String COLLECTION = "collection";

    private static final String SET = "set";

    private static final String LIST = "list";

    private static final String MAP = "map";

    private static final String ARRAY = "array";

    @Override
    public String style(@Nullable Object value) {
        if (value == null) {
            return NULL;
        } else if (value instanceof String) {
            return "\'" + value + "\'";
        } else if (value instanceof Class) {
            return ClassUtils.getShortName((Class<?>) value);
        } else if (value instanceof Method) {
            Method method = (Method) value;
            return method.getName() + "@" + ClassUtils.getShortName(method.getDeclaringClass());
        } else if (value instanceof Map) {
            return style((Map<?, ?>) value);
        } else if (value instanceof Map.Entry) {
            return style((Map.Entry<?, ?>) value);
        } else if (value instanceof Collection) {
            return style((Collection<?>) value);
        } else if (value.getClass().isArray()) {
            return styleArray(ObjectUtils.toObjectArray(value));
        } else {
            return String.valueOf(value);
        }
    }

    private <K, V> String style(Map<K, V> value) {
        StringBuilder result = new StringBuilder(value.size() * 8 + 16);
        result.append(MAP + "[");
        for (Iterator<Map.Entry<K, V>> it = value.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<K, V> entry = it.next();
            result.append(style(entry));
            if (it.hasNext()) {
                result.append(',').append(' ');
            }
        }
        if (value.isEmpty()) {
            result.append(EMPTY);
        }
        result.append("]");
        return result.toString();
    }

    private String style(Map.Entry<?, ?> value) {
        return style(value.getKey()) + " -> " + style(value.getValue());
    }

    private String style(Collection<?> value) {
        StringBuilder result = new StringBuilder(value.size() * 8 + 16);
        result.append(getCollectionTypeString(value)).append('[');
        for (Iterator<?> i = value.iterator(); i.hasNext(); ) {
            result.append(style(i.next()));
            if (i.hasNext()) {
                result.append(',').append(' ');
            }
        }
        if (value.isEmpty()) {
            result.append(EMPTY);
        }
        result.append("]");
        return result.toString();
    }

    private String getCollectionTypeString(Collection<?> value) {
        if (value instanceof List) {
            return LIST;
        } else if (value instanceof Set) {
            return SET;
        } else {
            return COLLECTION;
        }
    }

    private String styleArray(Object[] array) {
        StringBuilder result = new StringBuilder(array.length * 8 + 16);
        result.append(ARRAY + "<").append(ClassUtils.getShortName(array.getClass().getComponentType())).append(">[");
        for (int i = 0; i < array.length - 1; i++) {
            result.append(style(array[i]));
            result.append(',').append(' ');
        }
        if (array.length > 0) {
            result.append(style(array[array.length - 1]));
        } else {
            result.append(EMPTY);
        }
        result.append("]");
        return result.toString();
    }

}
