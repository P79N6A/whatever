package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;

final class BufferingClientHttpRequestWrapper extends AbstractBufferingClientHttpRequest {

    private final ClientHttpRequest request;

    BufferingClientHttpRequestWrapper(ClientHttpRequest request) {
        this.request = request;
    }

    @Override
    @Nullable
    public HttpMethod getMethod() {
        return this.request.getMethod();
    }

    @Override
    public String getMethodValue() {
        return this.request.getMethodValue();
    }

    @Override
    public URI getURI() {
        return this.request.getURI();
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
        this.request.getHeaders().putAll(headers);
        StreamUtils.copy(bufferedOutput, this.request.getBody());
        ClientHttpResponse response = this.request.execute();
        return new BufferingClientHttpResponseWrapper(response);
    }

}
