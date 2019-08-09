package org.springframework.http.converter;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class HttpMessageConversionException extends NestedRuntimeException {

    public HttpMessageConversionException(String msg) {
        super(msg);
    }

    public HttpMessageConversionException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

}
