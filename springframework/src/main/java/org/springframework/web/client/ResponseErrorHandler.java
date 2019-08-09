package org.springframework.web.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

public interface ResponseErrorHandler {

    boolean hasError(ClientHttpResponse response) throws IOException;

    void handleError(ClientHttpResponse response) throws IOException;

    default void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        handleError(response);
    }

}
