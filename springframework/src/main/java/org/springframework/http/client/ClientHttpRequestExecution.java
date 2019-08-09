package org.springframework.http.client;

import org.springframework.http.HttpRequest;

import java.io.IOException;

@FunctionalInterface
public interface ClientHttpRequestExecution {

    ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException;

}
