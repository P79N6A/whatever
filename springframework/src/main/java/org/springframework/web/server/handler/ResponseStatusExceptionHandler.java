package org.springframework.web.server.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

public class ResponseStatusExceptionHandler implements WebExceptionHandler {

    private static final Log logger = LogFactory.getLog(ResponseStatusExceptionHandler.class);

    @Nullable
    private Log warnLogger;

    public void setWarnLogCategory(String loggerName) {
        this.warnLogger = LogFactory.getLog(loggerName);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        if (status == null || !exchange.getResponse().setStatusCode(status)) {
            return Mono.error(ex);
        }
        // Mirrors AbstractHandlerExceptionResolver in spring-webmvc...
        String logPrefix = exchange.getLogPrefix();
        if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
            this.warnLogger.warn(logPrefix + formatError(ex, exchange.getRequest()), ex);
        } else if (logger.isDebugEnabled()) {
            logger.debug(logPrefix + formatError(ex, exchange.getRequest()));
        }
        return exchange.getResponse().setComplete();
    }

    private String formatError(Throwable ex, ServerHttpRequest request) {
        String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        String path = request.getURI().getRawPath();
        return "Resolved [" + reason + "] for HTTP " + request.getMethod() + " " + path;
    }

    @Nullable
    private HttpStatus resolveStatus(Throwable ex) {
        HttpStatus status = determineStatus(ex);
        if (status == null) {
            Throwable cause = ex.getCause();
            if (cause != null) {
                status = resolveStatus(cause);
            }
        }
        return status;
    }

    @Nullable
    protected HttpStatus determineStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            return ((ResponseStatusException) ex).getStatus();
        }
        return null;
    }

}
