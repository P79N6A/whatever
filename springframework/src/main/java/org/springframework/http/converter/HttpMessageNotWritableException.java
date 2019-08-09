package org.springframework.http.converter;

import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class HttpMessageNotWritableException extends HttpMessageConversionException {

    public HttpMessageNotWritableException(String msg) {
        super(msg);
    }

    public HttpMessageNotWritableException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

}
