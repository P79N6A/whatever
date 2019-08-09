package org.springframework.http;

import org.springframework.lang.Nullable;

import java.net.URI;

public interface HttpRequest extends HttpMessage {

    @Nullable
    default HttpMethod getMethod() {
        return HttpMethod.resolve(getMethodValue());
    }

    String getMethodValue();

    URI getURI();

}
