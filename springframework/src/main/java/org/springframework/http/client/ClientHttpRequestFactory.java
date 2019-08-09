package org.springframework.http.client;

import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;

@FunctionalInterface
public interface ClientHttpRequestFactory {

    ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
