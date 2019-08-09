package org.springframework.core.annotation;

import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public interface MergedAnnotation<A extends Annotation> {

    String VALUE = "value";

    Class<A> getType();

    List<Class<? extends Annotation>> getTypeHierarchy();

    boolean isPresent();

    boolean isDirectlyPresent();

    boolean isMetaPresent();

    int getDepth();

    int getAggregateIndex();

    @Nullable
    Object getSource();

    @Nullable
    MergedAnnotation<?> getParent();

    MergedAnnotation<?> getRoot();

    boolean hasNonDefaultValue(String attributeName);

    boolean hasDefaultValue(String attributeName) throws NoSuchElementException;

    byte getByte(String attributeName) throws NoSuchElementException;

    byte[] getByteArray(String attributeName) throws NoSuchElementException;

    boolean getBoolean(String attributeName) throws NoSuchElementException;

    boolean[] getBooleanArray(String attributeName) throws NoSuchElementException;

    char getChar(String attributeName) throws NoSuchElementException;

    char[] getCharArray(String attributeName) throws NoSuchElementException;

    short getShort(String attributeName) throws NoSuchElementException;

    short[] getShortArray(String attributeName) throws NoSuchElementException;

    int getInt(String attributeName) throws NoSuchElementException;

    int[] getIntArray(String attributeName) throws NoSuchElementException;

    long getLong(String attributeName) throws NoSuchElementException;

    long[] getLongArray(String attributeName) throws NoSuchElementException;

    double getDouble(String attributeName) throws NoSuchElementException;

    double[] getDoubleArray(String attributeName) throws NoSuchElementException;

    float getFloat(String attributeName) throws NoSuchElementException;

    float[] getFloatArray(String attributeName) throws NoSuchElementException;

    String getString(String attributeName) throws NoSuchElementException;

    String[] getStringArray(String attributeName) throws NoSuchElementException;

    Class<?> getClass(String attributeName) throws NoSuchElementException;

    Class<?>[] getClassArray(String attributeName) throws NoSuchElementException;

    <E extends Enum<E>> E getEnum(String attributeName, Class<E> type) throws NoSuchElementException;

    <E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) throws NoSuchElementException;

    <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type) throws NoSuchElementException;

    <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName, Class<T> type) throws NoSuchElementException;

    Optional<Object> getValue(String attributeName);

    <T> Optional<T> getValue(String attributeName, Class<T> type);

    Optional<Object> getDefaultValue(String attributeName);

    <T> Optional<T> getDefaultValue(String attributeName, Class<T> type);

    MergedAnnotation<A> filterDefaultValues();

    MergedAnnotation<A> filterAttributes(Predicate<String> predicate);

    MergedAnnotation<A> withNonMergedAttributes();

    AnnotationAttributes asAnnotationAttributes(Adapt... adaptations);

    Map<String, Object> asMap(Adapt... adaptations);

    <T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, Adapt... adaptations);

    A synthesize() throws NoSuchElementException;

    Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException;

    static <A extends Annotation> MergedAnnotation<A> missing() {
        return MissingMergedAnnotation.getInstance();
    }

    static <A extends Annotation> MergedAnnotation<A> from(A annotation) {
        return from(null, annotation);
    }

    static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source, A annotation) {
        return TypeMappedAnnotation.from(source, annotation);
    }

    static <A extends Annotation> MergedAnnotation<A> of(Class<A> annotationType) {
        return of(null, annotationType, null);
    }

    static <A extends Annotation> MergedAnnotation<A> of(Class<A> annotationType, @Nullable Map<String, ?> attributes) {
        return of(null, annotationType, attributes);
    }

    static <A extends Annotation> MergedAnnotation<A> of(@Nullable AnnotatedElement source, Class<A> annotationType, @Nullable Map<String, ?> attributes) {
        return of(null, source, annotationType, attributes);
    }

    static <A extends Annotation> MergedAnnotation<A> of(@Nullable ClassLoader classLoader, @Nullable Object source, Class<A> annotationType, @Nullable Map<String, ?> attributes) {
        return TypeMappedAnnotation.of(classLoader, source, annotationType, attributes);
    }

    enum Adapt {

        CLASS_TO_STRING,

        ANNOTATION_TO_MAP;

        protected final boolean isIn(Adapt... adaptations) {
            for (Adapt candidate : adaptations) {
                if (candidate == this) {
                    return true;
                }
            }
            return false;
        }

        public static Adapt[] values(boolean classToString, boolean annotationsToMap) {
            EnumSet<Adapt> result = EnumSet.noneOf(Adapt.class);
            addIfTrue(result, Adapt.CLASS_TO_STRING, classToString);
            addIfTrue(result, Adapt.ANNOTATION_TO_MAP, annotationsToMap);
            return result.toArray(new Adapt[0]);
        }

        private static <T> void addIfTrue(Set<T> result, T value, boolean test) {
            if (test) {
                result.add(value);
            }
        }
    }

}
