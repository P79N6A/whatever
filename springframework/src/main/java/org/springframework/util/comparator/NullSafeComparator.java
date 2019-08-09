package org.springframework.util.comparator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Comparator;

public class NullSafeComparator<T> implements Comparator<T> {

    @SuppressWarnings("rawtypes")
    public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<>(true);

    @SuppressWarnings("rawtypes")
    public static final NullSafeComparator NULLS_HIGH = new NullSafeComparator<>(false);

    private final Comparator<T> nonNullComparator;

    private final boolean nullsLow;

    @SuppressWarnings("unchecked")
    private NullSafeComparator(boolean nullsLow) {
        this.nonNullComparator = ComparableComparator.INSTANCE;
        this.nullsLow = nullsLow;
    }

    public NullSafeComparator(Comparator<T> comparator, boolean nullsLow) {
        Assert.notNull(comparator, "Non-null Comparator is required");
        this.nonNullComparator = comparator;
        this.nullsLow = nullsLow;
    }

    @Override
    public int compare(@Nullable T o1, @Nullable T o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return (this.nullsLow ? -1 : 1);
        }
        if (o2 == null) {
            return (this.nullsLow ? 1 : -1);
        }
        return this.nonNullComparator.compare(o1, o2);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NullSafeComparator)) {
            return false;
        }
        NullSafeComparator<T> otherComp = (NullSafeComparator<T>) other;
        return (this.nonNullComparator.equals(otherComp.nonNullComparator) && this.nullsLow == otherComp.nullsLow);
    }

    @Override
    public int hashCode() {
        return this.nonNullComparator.hashCode() * (this.nullsLow ? -1 : 1);
    }

    @Override
    public String toString() {
        return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " + (this.nullsLow ? "nulls low" : "nulls high");
    }

}
