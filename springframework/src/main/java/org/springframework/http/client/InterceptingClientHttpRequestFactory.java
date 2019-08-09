package org.springframework.http.client;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class InterceptingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

    private final List<ClientHttpRequestInterceptor> interceptors;

    public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory, @Nullable List<ClientHttpRequestInterceptor> interceptors) {
        super(requestFactory);
        this.interceptors = (interceptors != null ? interceptors : Collections.emptyList());
    }

    @Override
    protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {
        return new InterceptingClientHttpRequest(requestFactory, this.interceptors, uri, httpMethod);
    }

}
