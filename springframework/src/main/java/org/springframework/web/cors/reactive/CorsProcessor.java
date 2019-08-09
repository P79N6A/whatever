package org.springframework.web.cors.reactive;

import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

public interface CorsProcessor {

    boolean process(@Nullable CorsConfiguration configuration, ServerWebExchange exchange);

}
