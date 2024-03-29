package org.springframework.boot.context.properties.bind.validation;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.validation.FieldError;

final class OriginTrackedFieldError extends FieldError implements OriginProvider {

    private final Origin origin;

    private OriginTrackedFieldError(FieldError fieldError, Origin origin) {
        super(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(), fieldError.isBindingFailure(), fieldError.getCodes(), fieldError.getArguments(), fieldError.getDefaultMessage());
        this.origin = origin;
    }

    @Override
    public Origin getOrigin() {
        return this.origin;
    }

    @Override
    public String toString() {
        if (this.origin == null) {
            return super.toString();
        }
        return super.toString() + "; origin " + this.origin;
    }

    public static FieldError of(FieldError fieldError, Origin origin) {
        if (fieldError == null || origin == null) {
            return fieldError;
        }
        return new OriginTrackedFieldError(fieldError, origin);
    }

}
