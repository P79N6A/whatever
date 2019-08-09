package org.springframework.web;

import org.springframework.lang.Nullable;

import javax.servlet.ServletException;

@SuppressWarnings("serial")
public class HttpSessionRequiredException extends ServletException {

    @Nullable
    private final String expectedAttribute;

    public HttpSessionRequiredException(String msg) {
        super(msg);
        this.expectedAttribute = null;
    }

    public HttpSessionRequiredException(String msg, String expectedAttribute) {
        super(msg);
        this.expectedAttribute = expectedAttribute;
    }

    @Nullable
    public String getExpectedAttribute() {
        return this.expectedAttribute;
    }

}
