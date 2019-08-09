package org.springframework.boot.origin;

import org.springframework.util.ObjectUtils;

public class OriginTrackedValue implements OriginProvider {

    private final Object value;

    private final Origin origin;

    private OriginTrackedValue(Object value, Origin origin) {
        this.value = value;
        this.origin = origin;
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public Origin getOrigin() {
        return this.origin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(this.value, ((OriginTrackedValue) obj).value);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return (this.value != null) ? this.value.toString() : null;
    }

    public static OriginTrackedValue of(Object value) {
        return of(value, null);
    }

    public static OriginTrackedValue of(Object value, Origin origin) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence) {
            return new OriginTrackedCharSequence((CharSequence) value, origin);
        }
        return new OriginTrackedValue(value, origin);
    }

    private static class OriginTrackedCharSequence extends OriginTrackedValue implements CharSequence {

        OriginTrackedCharSequence(CharSequence value, Origin origin) {
            super(value, origin);
        }

        @Override
        public int length() {
            return getValue().length();
        }

        @Override
        public char charAt(int index) {
            return getValue().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return getValue().subSequence(start, end);
        }

        @Override
        public CharSequence getValue() {
            return (CharSequence) super.getValue();
        }

    }

}
