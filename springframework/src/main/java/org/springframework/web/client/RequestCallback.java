package org.springframework.web.client;

import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;

@FunctionalInterface
public interface RequestCallback {

    void doWithRequest(ClientHttpRequest request) throws IOException;

}
