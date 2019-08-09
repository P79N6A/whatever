package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public class ServerHttpResponseDecorator implements ServerHttpResponse {

    private final ServerHttpResponse delegate;

    public ServerHttpResponseDecorator(ServerHttpResponse delegate) {
        Assert.notNull(delegate, "Delegate is required");
        this.delegate = delegate;
    }

    public ServerHttpResponse getDelegate() {
        return this.delegate;
    }
    // ServerHttpResponse delegation methods...

    @Override
    public boolean setStatusCode(@Nullable HttpStatus status) {
        return getDelegate().setStatusCode(status);
    }

    @Override
    public HttpStatus getStatusCode() {
        return getDelegate().getStatusCode();
    }

    @Override
    public HttpHeaders getHeaders() {
        return getDelegate().getHeaders();
    }

    @Override
    public MultiValueMap<String, ResponseCookie> getCookies() {
        return getDelegate().getCookies();
    }

    @Override
    public void addCookie(ResponseCookie cookie) {
        getDelegate().addCookie(cookie);
    }

    @Override
    public DataBufferFactory bufferFactory() {
        return getDelegate().bufferFactory();
    }

    @Override
    public void beforeCommit(Supplier<? extends Mono<Void>> action) {
        getDelegate().beforeCommit(action);
    }

    @Override
    public boolean isCommitted() {
        return getDelegate().isCommitted();
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        return getDelegate().writeWith(body);
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return getDelegate().writeAndFlushWith(body);
    }

    @Override
    public Mono<Void> setComplete() {
        return getDelegate().setComplete();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
    }

}
