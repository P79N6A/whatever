package org.springframework.http.client.support;

import org.apache.commons.logging.Log;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;

public abstract class HttpAccessor {

    protected final Log logger = HttpLogging.forLogName(getClass());

    private ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
        Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
        this.requestFactory = requestFactory;
    }

    public ClientHttpRequestFactory getRequestFactory() {
        return this.requestFactory;
    }

    protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
        ClientHttpRequest request = getRequestFactory().createRequest(url, method);
        if (logger.isDebugEnabled()) {
            logger.debug("HTTP " + method.name() + " " + url);
        }
        return request;
    }

}
