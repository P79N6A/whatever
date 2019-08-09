package org.springframework.http.server.reactive;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.net.URI;

public class ServerHttpRequestDecorator implements ServerHttpRequest {

    private final ServerHttpRequest delegate;

    public ServerHttpRequestDecorator(ServerHttpRequest delegate) {
        Assert.notNull(delegate, "Delegate is required");
        this.delegate = delegate;
    }

    public ServerHttpRequest getDelegate() {
        return this.delegate;
    }
    // ServerHttpRequest delegation methods...

    @Override
    public String getId() {
        return getDelegate().getId();
    }

    @Override
    @Nullable
    public HttpMethod getMethod() {
        return getDelegate().getMethod();
    }

    @Override
    public String getMethodValue() {
        return getDelegate().getMethodValue();
    }

    @Override
    public URI getURI() {
        return getDelegate().getURI();
    }

    @Override
    public RequestPath getPath() {
        return getDelegate().getPath();
    }

    @Override
    public MultiValueMap<String, String> getQueryParams() {
        return getDelegate().getQueryParams();
    }

    @Override
    public HttpHeaders getHeaders() {
        return getDelegate().getHeaders();
    }

    @Override
    public MultiValueMap<String, HttpCookie> getCookies() {
        return getDelegate().getCookies();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return getDelegate().getRemoteAddress();
    }

    @Nullable
    @Override
    public SslInfo getSslInfo() {
        return getDelegate().getSslInfo();
    }

    @Override
    public Flux<DataBuffer> getBody() {
        return getDelegate().getBody();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
    }

}
