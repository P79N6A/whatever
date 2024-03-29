package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import java.util.*;

public final class CollectionFactory {

    private static final Set<Class<?>> approximableCollectionTypes = new HashSet<>();

    private static final Set<Class<?>> approximableMapTypes = new HashSet<>();

    static {
        // Standard collection interfaces
        approximableCollectionTypes.add(Collection.class);
        approximableCollectionTypes.add(List.class);
        approximableCollectionTypes.add(Set.class);
        approximableCollectionTypes.add(SortedSet.class);
        approximableCollectionTypes.add(NavigableSet.class);
        approximableMapTypes.add(Map.class);
        approximableMapTypes.add(SortedMap.class);
        approximableMapTypes.add(NavigableMap.class);
        // Common concrete collection classes
        approximableCollectionTypes.add(ArrayList.class);
        approximableCollectionTypes.add(LinkedList.class);
        approximableCollectionTypes.add(HashSet.class);
        approximableCollectionTypes.add(LinkedHashSet.class);
        approximableCollectionTypes.add(TreeSet.class);
        approximableCollectionTypes.add(EnumSet.class);
        approximableMapTypes.add(HashMap.class);
        approximableMapTypes.add(LinkedHashMap.class);
        approximableMapTypes.add(TreeMap.class);
        approximableMapTypes.add(EnumMap.class);
    }

    private CollectionFactory() {
    }

    public static boolean isApproximableCollectionType(@Nullable Class<?> collectionType) {
        return (collectionType != null && approximableCollectionTypes.contains(collectionType));
    }

    @SuppressWarnings({"unchecked", "cast", "rawtypes"})
    public static <E> Collection<E> createApproximateCollection(@Nullable Object collection, int capacity) {
        if (collection instanceof LinkedList) {
            return new LinkedList<>();
        } else if (collection instanceof List) {
            return new ArrayList<>(capacity);
        } else if (collection instanceof EnumSet) {
            // Cast is necessary for compilation in Eclipse 4.4.1.
            Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
            enumSet.clear();
            return enumSet;
        } else if (collection instanceof SortedSet) {
            return new TreeSet<>(((SortedSet<E>) collection).comparator());
        } else {
            return new LinkedHashSet<>(capacity);
        }
    }

    public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
        return createCollection(collectionType, null, capacity);
    }

    @SuppressWarnings({"unchecked", "cast"})
    public static <E> Collection<E> createCollection(Class<?> collectionType, @Nullable Class<?> elementType, int capacity) {
        Assert.notNull(collectionType, "Collection type must not be null");
        if (collectionType.isInterface()) {
            if (Set.class == collectionType || Collection.class == collectionType) {
                return new LinkedHashSet<>(capacity);
            } else if (List.class == collectionType) {
                return new ArrayList<>(capacity);
            } else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
                return new TreeSet<>();
            } else {
                throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
            }
        } else if (EnumSet.class.isAssignableFrom(collectionType)) {
            Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
            // Cast is necessary for compilation in Eclipse 4.4.1.
            return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
        } else {
            if (!Collection.class.isAssignableFrom(collectionType)) {
                throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
            }
            try {
                return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate Collection type: " + collectionType.getName(), ex);
            }
        }
    }

    public static boolean isApproximableMapType(@Nullable Class<?> mapType) {
        return (mapType != null && approximableMapTypes.contains(mapType));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map, int capacity) {
        if (map instanceof EnumMap) {
            EnumMap enumMap = new EnumMap((EnumMap) map);
            enumMap.clear();
            return enumMap;
        } else if (map instanceof SortedMap) {
            return new TreeMap<>(((SortedMap<K, V>) map).comparator());
        } else {
            return new LinkedHashMap<>(capacity);
        }
    }

    public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
        return createMap(mapType, null, capacity);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Map<K, V> createMap(Class<?> mapType, @Nullable Class<?> keyType, int capacity) {
        Assert.notNull(mapType, "Map type must not be null");
        if (mapType.isInterface()) {
            if (Map.class == mapType) {
                return new LinkedHashMap<>(capacity);
            } else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
                return new TreeMap<>();
            } else if (MultiValueMap.class == mapType) {
                return new LinkedMultiValueMap();
            } else {
                throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
            }
        } else if (EnumMap.class == mapType) {
            Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
            return new EnumMap(asEnumType(keyType));
        } else {
            if (!Map.class.isAssignableFrom(mapType)) {
                throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
            }
            try {
                return (Map<K, V>) ReflectionUtils.accessibleConstructor(mapType).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
            }
        }
    }

    @SuppressWarnings("serial")
    public static Properties createStringAdaptingProperties() {
        return new Properties() {
            @Override
            @Nullable
            public String getProperty(String key) {
                Object value = get(key);
                return (value != null ? value.toString() : null);
            }
        };
    }

    @SuppressWarnings("rawtypes")
    private static Class<? extends Enum> asEnumType(Class<?> enumType) {
        Assert.notNull(enumType, "Enum type must not be null");
        if (!Enum.class.isAssignableFrom(enumType)) {
            throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
        }
        return enumType.asSubclass(Enum.class);
    }

}
