package org.springframework.http.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@SuppressWarnings("serial")
public class HttpMessageNotReadableException extends HttpMessageConversionException {

    @Nullable
    private final HttpInputMessage httpInputMessage;

    @Deprecated
    public HttpMessageNotReadableException(String msg) {
        super(msg);
        this.httpInputMessage = null;
    }

    @Deprecated
    public HttpMessageNotReadableException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
        this.httpInputMessage = null;
    }

    public HttpMessageNotReadableException(String msg, HttpInputMessage httpInputMessage) {
        super(msg);
        this.httpInputMessage = httpInputMessage;
    }

    public HttpMessageNotReadableException(String msg, @Nullable Throwable cause, HttpInputMessage httpInputMessage) {
        super(msg, cause);
        this.httpInputMessage = httpInputMessage;
    }

    public HttpInputMessage getHttpInputMessage() {
        Assert.state(this.httpInputMessage != null, "No HttpInputMessage available - use non-deprecated constructors");
        return this.httpInputMessage;
    }

}
