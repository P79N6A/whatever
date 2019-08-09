package org.springframework.web.util;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.lang.Nullable;

import javax.servlet.ServletException;

public class NestedServletException extends ServletException {

    private static final long serialVersionUID = -5292377985529381145L;

    static {
        // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
        // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
        NestedExceptionUtils.class.getName();
    }

    public NestedServletException(String msg) {
        super(msg);
    }

    public NestedServletException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

    @Override
    @Nullable
    public String getMessage() {
        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
    }

}
