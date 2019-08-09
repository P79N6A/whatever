package org.springframework.http.client;

import org.springframework.http.HttpStatus;

import java.io.IOException;

public abstract class AbstractClientHttpResponse implements ClientHttpResponse {

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return HttpStatus.valueOf(getRawStatusCode());
    }

}
