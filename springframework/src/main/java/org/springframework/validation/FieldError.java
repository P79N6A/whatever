package org.springframework.validation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

@SuppressWarnings("serial")
public class FieldError extends ObjectError {

    private final String field;

    @Nullable
    private final Object rejectedValue;

    private final boolean bindingFailure;

    public FieldError(String objectName, String field, String defaultMessage) {
        this(objectName, field, null, false, null, null, defaultMessage);
    }

    public FieldError(String objectName, String field, @Nullable Object rejectedValue, boolean bindingFailure, @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {
        super(objectName, codes, arguments, defaultMessage);
        Assert.notNull(field, "Field must not be null");
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.bindingFailure = bindingFailure;
    }

    public String getField() {
        return this.field;
    }

    @Nullable
    public Object getRejectedValue() {
        return this.rejectedValue;
    }

    public boolean isBindingFailure() {
        return this.bindingFailure;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        FieldError otherError = (FieldError) other;
        return (getField().equals(otherError.getField()) && ObjectUtils.nullSafeEquals(getRejectedValue(), otherError.getRejectedValue()) && isBindingFailure() == otherError.isBindingFailure());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 29 * hashCode + getField().hashCode();
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getRejectedValue());
        hashCode = 29 * hashCode + (isBindingFailure() ? 1 : 0);
        return hashCode;
    }

    @Override
    public String toString() {
        return "Field error in object '" + getObjectName() + "' on field '" + this.field + "': rejected value [" + ObjectUtils.nullSafeToString(this.rejectedValue) + "]; " + resolvableToString();
    }

}
