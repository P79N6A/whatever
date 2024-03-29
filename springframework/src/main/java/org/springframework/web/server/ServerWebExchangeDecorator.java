package org.springframework.web.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

public class ServerWebExchangeDecorator implements ServerWebExchange {

    private final ServerWebExchange delegate;

    protected ServerWebExchangeDecorator(ServerWebExchange delegate) {
        Assert.notNull(delegate, "ServerWebExchange 'delegate' is required.");
        this.delegate = delegate;
    }

    public ServerWebExchange getDelegate() {
        return this.delegate;
    }
    // ServerWebExchange delegation methods...

    @Override
    public ServerHttpRequest getRequest() {
        return getDelegate().getRequest();
    }

    @Override
    public ServerHttpResponse getResponse() {
        return getDelegate().getResponse();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return getDelegate().getAttributes();
    }

    @Override
    public Mono<WebSession> getSession() {
        return getDelegate().getSession();
    }

    @Override
    public <T extends Principal> Mono<T> getPrincipal() {
        return getDelegate().getPrincipal();
    }

    @Override
    public LocaleContext getLocaleContext() {
        return getDelegate().getLocaleContext();
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return getDelegate().getApplicationContext();
    }

    @Override
    public Mono<MultiValueMap<String, String>> getFormData() {
        return getDelegate().getFormData();
    }

    @Override
    public Mono<MultiValueMap<String, Part>> getMultipartData() {
        return getDelegate().getMultipartData();
    }

    @Override
    public boolean isNotModified() {
        return getDelegate().isNotModified();
    }

    @Override
    public boolean checkNotModified(Instant lastModified) {
        return getDelegate().checkNotModified(lastModified);
    }

    @Override
    public boolean checkNotModified(String etag) {
        return getDelegate().checkNotModified(etag);
    }

    @Override
    public boolean checkNotModified(@Nullable String etag, Instant lastModified) {
        return getDelegate().checkNotModified(etag, lastModified);
    }

    @Override
    public String transformUrl(String url) {
        return getDelegate().transformUrl(url);
    }

    @Override
    public void addUrlTransformer(Function<String, String> transformer) {
        getDelegate().addUrlTransformer(transformer);
    }

    @Override
    public String getLogPrefix() {
        return getDelegate().getLogPrefix();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
    }

}
