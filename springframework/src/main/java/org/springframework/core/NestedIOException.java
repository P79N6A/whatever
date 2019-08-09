package org.springframework.core;

import org.springframework.lang.Nullable;

import java.io.IOException;

@SuppressWarnings("serial")
public class NestedIOException extends IOException {

    static {
        // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
        // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
        NestedExceptionUtils.class.getName();
    }

    public NestedIOException(String msg) {
        super(msg);
    }

    public NestedIOException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

    @Override
    @Nullable
    public String getMessage() {
        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
    }

}
