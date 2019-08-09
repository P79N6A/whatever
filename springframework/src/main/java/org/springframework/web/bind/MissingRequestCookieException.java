package org.springframework.web.bind;

import org.springframework.core.MethodParameter;

@SuppressWarnings("serial")
public class MissingRequestCookieException extends ServletRequestBindingException {

    private final String cookieName;

    private final MethodParameter parameter;

    public MissingRequestCookieException(String cookieName, MethodParameter parameter) {
        super("");
        this.cookieName = cookieName;
        this.parameter = parameter;
    }

    @Override
    public String getMessage() {
        return "Missing cookie '" + this.cookieName + "' for method parameter of type " + this.parameter.getNestedParameterType().getSimpleName();
    }

    public final String getCookieName() {
        return this.cookieName;
    }

    public final MethodParameter getParameter() {
        return this.parameter;
    }

}
