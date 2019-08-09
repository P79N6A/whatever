package org.springframework.web.filter.reactive;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import reactor.core.publisher.Mono;

@Deprecated
public class ForwardedHeaderFilter extends ForwardedHeaderTransformer implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (hasForwardedHeaders(request)) {
            exchange = exchange.mutate().request(apply(request)).build();
        }
        return chain.filter(exchange);
    }

}
