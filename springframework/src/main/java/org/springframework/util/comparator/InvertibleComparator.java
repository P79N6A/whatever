package org.springframework.util.comparator;

import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Comparator;

@Deprecated
@SuppressWarnings("serial")
public class InvertibleComparator<T> implements Comparator<T>, Serializable {

    private final Comparator<T> comparator;

    private boolean ascending = true;

    public InvertibleComparator(Comparator<T> comparator) {
        Assert.notNull(comparator, "Comparator must not be null");
        this.comparator = comparator;
    }

    public InvertibleComparator(Comparator<T> comparator, boolean ascending) {
        Assert.notNull(comparator, "Comparator must not be null");
        this.comparator = comparator;
        setAscending(ascending);
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public boolean isAscending() {
        return this.ascending;
    }

    public void invertOrder() {
        this.ascending = !this.ascending;
    }

    @Override
    public int compare(T o1, T o2) {
        int result = this.comparator.compare(o1, o2);
        if (result != 0) {
            // Invert the order if it is a reverse sort.
            if (!this.ascending) {
                if (Integer.MIN_VALUE == result) {
                    result = Integer.MAX_VALUE;
                } else {
                    result *= -1;
                }
            }
            return result;
        }
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InvertibleComparator)) {
            return false;
        }
        InvertibleComparator<T> otherComp = (InvertibleComparator<T>) other;
        return (this.comparator.equals(otherComp.comparator) && this.ascending == otherComp.ascending);
    }

    @Override
    public int hashCode() {
        return this.comparator.hashCode();
    }

    @Override
    public String toString() {
        return "InvertibleComparator: [" + this.comparator + "]; ascending=" + this.ascending;
    }

}
