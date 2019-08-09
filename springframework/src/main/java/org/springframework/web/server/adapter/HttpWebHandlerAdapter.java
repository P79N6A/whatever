package org.springframework.web.server.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.WebHandlerDecorator;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HttpWebHandlerAdapter extends WebHandlerDecorator implements HttpHandler {

    private static final String DISCONNECTED_CLIENT_LOG_CATEGORY = "org.springframework.web.server.DisconnectedClient";

    // Similar declaration exists in AbstractSockJsSession..
    private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = new HashSet<>(Arrays.asList("AbortedException", "ClientAbortException", "EOFException", "EofException"));

    private static final Log logger = LogFactory.getLog(HttpWebHandlerAdapter.class);

    private static final Log lostClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);

    private WebSessionManager sessionManager = new DefaultWebSessionManager();

    private ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();

    private LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();

    @Nullable
    private ForwardedHeaderTransformer forwardedHeaderTransformer;

    @Nullable
    private ApplicationContext applicationContext;

    private boolean enableLoggingRequestDetails = false;

    public HttpWebHandlerAdapter(WebHandler delegate) {
        super(delegate);
    }

    public void setSessionManager(WebSessionManager sessionManager) {
        Assert.notNull(sessionManager, "WebSessionManager must not be null");
        this.sessionManager = sessionManager;
    }

    public WebSessionManager getSessionManager() {
        return this.sessionManager;
    }

    public void setCodecConfigurer(ServerCodecConfigurer codecConfigurer) {
        Assert.notNull(codecConfigurer, "ServerCodecConfigurer is required");
        this.codecConfigurer = codecConfigurer;
        this.enableLoggingRequestDetails = false;
        this.codecConfigurer.getReaders().stream().filter(LoggingCodecSupport.class::isInstance).forEach(reader -> {
            if (((LoggingCodecSupport) reader).isEnableLoggingRequestDetails()) {
                this.enableLoggingRequestDetails = true;
            }
        });
    }

    public ServerCodecConfigurer getCodecConfigurer() {
        return this.codecConfigurer;
    }

    public void setLocaleContextResolver(LocaleContextResolver resolver) {
        Assert.notNull(resolver, "LocaleContextResolver is required");
        this.localeContextResolver = resolver;
    }

    public LocaleContextResolver getLocaleContextResolver() {
        return this.localeContextResolver;
    }

    public void setForwardedHeaderTransformer(ForwardedHeaderTransformer transformer) {
        Assert.notNull(transformer, "ForwardedHeaderTransformer is required");
        this.forwardedHeaderTransformer = transformer;
    }

    @Nullable
    public ForwardedHeaderTransformer getForwardedHeaderTransformer() {
        return this.forwardedHeaderTransformer;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Nullable
    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    public void afterPropertiesSet() {
        if (logger.isDebugEnabled()) {
            String value = this.enableLoggingRequestDetails ? "shown which may lead to unsafe logging of potentially sensitive data" : "masked to prevent unsafe logging of potentially sensitive data";
            logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails + "': form data and headers will be " + value);
        }
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
        if (this.forwardedHeaderTransformer != null) {
            request = this.forwardedHeaderTransformer.apply(request);
        }
        ServerWebExchange exchange = createExchange(request, response);
        LogFormatUtils.traceDebug(logger, traceOn -> exchange.getLogPrefix() + formatRequest(exchange.getRequest()) + (traceOn ? ", headers=" + formatHeaders(exchange.getRequest().getHeaders()) : ""));
        return getDelegate().handle(exchange).doOnSuccess(aVoid -> logResponse(exchange)).onErrorResume(ex -> handleUnresolvedError(exchange, ex)).then(Mono.defer(response::setComplete));
    }

    protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
        return new DefaultServerWebExchange(request, response, this.sessionManager, getCodecConfigurer(), getLocaleContextResolver(), this.applicationContext);
    }

    private String formatRequest(ServerHttpRequest request) {
        String rawQuery = request.getURI().getRawQuery();
        String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
        return "HTTP " + request.getMethod() + " \"" + request.getPath() + query + "\"";
    }

    private void logResponse(ServerWebExchange exchange) {
        LogFormatUtils.traceDebug(logger, traceOn -> {
            HttpStatus status = exchange.getResponse().getStatusCode();
            return exchange.getLogPrefix() + "Completed " + (status != null ? status : "200 OK") + (traceOn ? ", headers=" + formatHeaders(exchange.getResponse().getHeaders()) : "");
        });
    }

    private String formatHeaders(HttpHeaders responseHeaders) {
        return this.enableLoggingRequestDetails ? responseHeaders.toString() : responseHeaders.isEmpty() ? "{}" : "{masked}";
    }

    private Mono<Void> handleUnresolvedError(ServerWebExchange exchange, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String logPrefix = exchange.getLogPrefix();
        if (isDisconnectedClientError(ex)) {
            if (lostClientLogger.isTraceEnabled()) {
                lostClientLogger.trace(logPrefix + "Client went away", ex);
            } else if (lostClientLogger.isDebugEnabled()) {
                lostClientLogger.debug(logPrefix + "Client went away: " + ex + " (stacktrace at TRACE level for '" + DISCONNECTED_CLIENT_LOG_CATEGORY + "')");
            }
            return Mono.empty();
        } else if (response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)) {
            logger.error(logPrefix + "500 Server Error for " + formatRequest(request), ex);
            return Mono.empty();
        } else {
            // After the response is committed, propagate errors to the server...
            logger.error(logPrefix + "Error [" + ex + "] for " + formatRequest(request) + ", but ServerHttpResponse already committed (" + response.getStatusCode() + ")");
            return Mono.error(ex);
        }
    }

    private boolean isDisconnectedClientError(Throwable ex) {
        String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        if (message != null) {
            String text = message.toLowerCase();
            if (text.contains("broken pipe") || text.contains("connection reset by peer")) {
                return true;
            }
        }
        return DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName());
    }

}
