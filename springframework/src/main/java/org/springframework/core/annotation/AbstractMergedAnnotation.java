package org.springframework.core.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

abstract class AbstractMergedAnnotation<A extends Annotation> implements MergedAnnotation<A> {

    @Nullable
    private volatile A synthesizedAnnotation;

    @Override
    public boolean isDirectlyPresent() {
        return isPresent() && getDepth() == 0;
    }

    @Override
    public boolean isMetaPresent() {
        return isPresent() && getDepth() > 0;
    }

    @Override
    public boolean hasNonDefaultValue(String attributeName) {
        return !hasDefaultValue(attributeName);
    }

    public byte getByte(String attributeName) {
        return getRequiredAttributeValue(attributeName, Byte.class);
    }

    public byte[] getByteArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, byte[].class);
    }

    public boolean getBoolean(String attributeName) {
        return getRequiredAttributeValue(attributeName, Boolean.class);
    }

    public boolean[] getBooleanArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, boolean[].class);
    }

    public char getChar(String attributeName) {
        return getRequiredAttributeValue(attributeName, Character.class);
    }

    public char[] getCharArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, char[].class);
    }

    public short getShort(String attributeName) {
        return getRequiredAttributeValue(attributeName, Short.class);
    }

    public short[] getShortArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, short[].class);
    }

    public int getInt(String attributeName) {
        return getRequiredAttributeValue(attributeName, Integer.class);
    }

    public int[] getIntArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, int[].class);
    }

    public long getLong(String attributeName) {
        return getRequiredAttributeValue(attributeName, Long.class);
    }

    public long[] getLongArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, long[].class);
    }

    public double getDouble(String attributeName) {
        return getRequiredAttributeValue(attributeName, Double.class);
    }

    public double[] getDoubleArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, double[].class);
    }

    public float getFloat(String attributeName) {
        return getRequiredAttributeValue(attributeName, Float.class);
    }

    public float[] getFloatArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, float[].class);
    }

    public String getString(String attributeName) {
        return getRequiredAttributeValue(attributeName, String.class);
    }

    public String[] getStringArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, String[].class);
    }

    public Class<?> getClass(String attributeName) {
        return getRequiredAttributeValue(attributeName, Class.class);
    }

    public Class<?>[] getClassArray(String attributeName) {
        return getRequiredAttributeValue(attributeName, Class[].class);
    }

    public <E extends Enum<E>> E getEnum(String attributeName, Class<E> type) {
        Assert.notNull(type, "Type must not be null");
        return getRequiredAttributeValue(attributeName, type);
    }

    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) {
        Assert.notNull(type, "Type must not be null");
        Class<?> arrayType = Array.newInstance(type, 0).getClass();
        return (E[]) getRequiredAttributeValue(attributeName, arrayType);
    }

    @Override
    public Optional<Object> getValue(String attributeName) {
        return getValue(attributeName, Object.class);
    }

    @Override
    public <T> Optional<T> getValue(String attributeName, Class<T> type) {
        return Optional.ofNullable(getAttributeValue(attributeName, type));
    }

    @Override
    public Optional<Object> getDefaultValue(String attributeName) {
        return getDefaultValue(attributeName, Object.class);
    }

    @Override
    public MergedAnnotation<A> filterDefaultValues() {
        return filterAttributes(this::hasNonDefaultValue);
    }

    @Override
    public AnnotationAttributes asAnnotationAttributes(Adapt... adaptations) {
        return asMap(mergedAnnotation -> new AnnotationAttributes(mergedAnnotation.getType()), adaptations);
    }

    @Override
    public Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException {
        return (condition.test(this) ? Optional.of(synthesize()) : Optional.empty());
    }

    @Override
    public A synthesize() {
        if (!isPresent()) {
            throw new NoSuchElementException("Unable to synthesize missing annotation");
        }
        A synthesized = this.synthesizedAnnotation;
        if (synthesized == null) {
            synthesized = createSynthesized();
            this.synthesizedAnnotation = synthesized;
        }
        return synthesized;
    }

    private <T> T getRequiredAttributeValue(String attributeName, Class<T> type) {
        T value = getAttributeValue(attributeName, type);
        if (value == null) {
            throw new NoSuchElementException("No attribute named '" + attributeName + "' present in merged annotation " + getType().getName());
        }
        return value;
    }

    @Nullable
    protected abstract <T> T getAttributeValue(String attributeName, Class<T> type);

    protected abstract A createSynthesized();

}
