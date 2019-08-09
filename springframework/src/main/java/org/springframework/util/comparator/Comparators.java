package org.springframework.util.comparator;

import java.util.Comparator;

public abstract class Comparators {

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> comparable() {
        return ComparableComparator.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullsLow() {
        return NullSafeComparator.NULLS_LOW;
    }

    public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
        return new NullSafeComparator<>(comparator, true);
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullsHigh() {
        return NullSafeComparator.NULLS_HIGH;
    }

    public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
        return new NullSafeComparator<>(comparator, false);
    }

}
