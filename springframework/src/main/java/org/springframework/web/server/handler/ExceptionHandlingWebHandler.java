package org.springframework.web.server.handler;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExceptionHandlingWebHandler extends WebHandlerDecorator {

    private final List<WebExceptionHandler> exceptionHandlers;

    public ExceptionHandlingWebHandler(WebHandler delegate, List<WebExceptionHandler> handlers) {
        super(delegate);
        List<WebExceptionHandler> handlersToUse = new ArrayList<>();
        handlersToUse.add(new CheckpointInsertingHandler());
        handlersToUse.addAll(handlers);
        this.exceptionHandlers = Collections.unmodifiableList(handlersToUse);
    }

    public List<WebExceptionHandler> getExceptionHandlers() {
        return this.exceptionHandlers;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        Mono<Void> completion;
        try {
            completion = super.handle(exchange);
        } catch (Throwable ex) {
            completion = Mono.error(ex);
        }
        for (WebExceptionHandler handler : this.exceptionHandlers) {
            completion = completion.onErrorResume(ex -> handler.handle(exchange, ex));
        }
        return completion;
    }

    private static class CheckpointInsertingHandler implements WebExceptionHandler {

        @Override
        public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
            ServerHttpRequest request = exchange.getRequest();
            String rawQuery = request.getURI().getRawQuery();
            String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
            HttpMethod httpMethod = request.getMethod();
            String description = "HTTP " + httpMethod + " \"" + request.getPath() + query + "\"";
            return Mono.error(ex).checkpoint(description + " [ExceptionHandlingWebHandler]").cast(Void.class);
        }

    }

}
