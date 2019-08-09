package org.springframework.web.server;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.function.Consumer;

class DefaultServerWebExchangeBuilder implements ServerWebExchange.Builder {

    private final ServerWebExchange delegate;

    @Nullable
    private ServerHttpRequest request;

    @Nullable
    private ServerHttpResponse response;

    @Nullable
    private Mono<Principal> principalMono;

    DefaultServerWebExchangeBuilder(ServerWebExchange delegate) {
        Assert.notNull(delegate, "Delegate is required");
        this.delegate = delegate;
    }

    @Override
    public ServerWebExchange.Builder request(Consumer<ServerHttpRequest.Builder> consumer) {
        ServerHttpRequest.Builder builder = this.delegate.getRequest().mutate();
        consumer.accept(builder);
        return request(builder.build());
    }

    @Override
    public ServerWebExchange.Builder request(ServerHttpRequest request) {
        this.request = request;
        return this;
    }

    @Override
    public ServerWebExchange.Builder response(ServerHttpResponse response) {
        this.response = response;
        return this;
    }

    @Override
    public ServerWebExchange.Builder principal(Mono<Principal> principalMono) {
        this.principalMono = principalMono;
        return this;
    }

    @Override
    public ServerWebExchange build() {
        return new MutativeDecorator(this.delegate, this.request, this.response, this.principalMono);
    }

    private static class MutativeDecorator extends ServerWebExchangeDecorator {

        @Nullable
        private final ServerHttpRequest request;

        @Nullable
        private final ServerHttpResponse response;

        @Nullable
        private final Mono<Principal> principalMono;

        public MutativeDecorator(ServerWebExchange delegate, @Nullable ServerHttpRequest request, @Nullable ServerHttpResponse response, @Nullable Mono<Principal> principalMono) {
            super(delegate);
            this.request = request;
            this.response = response;
            this.principalMono = principalMono;
        }

        @Override
        public ServerHttpRequest getRequest() {
            return (this.request != null ? this.request : getDelegate().getRequest());
        }

        @Override
        public ServerHttpResponse getResponse() {
            return (this.response != null ? this.response : getDelegate().getResponse());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Principal> Mono<T> getPrincipal() {
            return (this.principalMono != null ? (Mono<T>) this.principalMono : getDelegate().getPrincipal());
        }

    }

}

