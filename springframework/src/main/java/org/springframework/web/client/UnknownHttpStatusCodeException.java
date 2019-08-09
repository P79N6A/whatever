package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;

public class UnknownHttpStatusCodeException extends RestClientResponseException {

    private static final long serialVersionUID = 7103980251635005491L;

    public UnknownHttpStatusCodeException(int rawStatusCode, String statusText, @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {
        super("Unknown status code [" + rawStatusCode + "]" + " " + statusText, rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
    }

}
