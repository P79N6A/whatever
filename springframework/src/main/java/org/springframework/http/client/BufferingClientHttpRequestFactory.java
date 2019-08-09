package org.springframework.http.client;

import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.URI;

public class BufferingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

    public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
        super(requestFactory);
    }

    @Override
    protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException {
        ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
        if (shouldBuffer(uri, httpMethod)) {
            return new BufferingClientHttpRequestWrapper(request);
        } else {
            return request;
        }
    }

    protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
        return true;
    }

}
