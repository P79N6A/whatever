package org.springframework.http.client;

import org.springframework.http.HttpRequest;

import java.io.IOException;

@FunctionalInterface
public interface ClientHttpRequestInterceptor {

    ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException;

}
