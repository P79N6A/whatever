package org.springframework.web.bind;

import org.springframework.core.MethodParameter;

@SuppressWarnings("serial")
public class MissingRequestHeaderException extends ServletRequestBindingException {

    private final String headerName;

    private final MethodParameter parameter;

    public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
        super("");
        this.headerName = headerName;
        this.parameter = parameter;
    }

    @Override
    public String getMessage() {
        return "Missing request header '" + this.headerName + "' for method parameter of type " + this.parameter.getNestedParameterType().getSimpleName();
    }

    public final String getHeaderName() {
        return this.headerName;
    }

    public final MethodParameter getParameter() {
        return this.parameter;
    }

}
