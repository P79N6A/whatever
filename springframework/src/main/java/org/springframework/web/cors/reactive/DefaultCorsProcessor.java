package org.springframework.web.cors.reactive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultCorsProcessor implements CorsProcessor {

    private static final Log logger = LogFactory.getLog(DefaultCorsProcessor.class);

    private static final List<String> VARY_HEADERS = Arrays.asList(HttpHeaders.ORIGIN, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

    @Override
    public boolean process(@Nullable CorsConfiguration config, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().addAll(HttpHeaders.VARY, VARY_HEADERS);
        if (!CorsUtils.isCorsRequest(request)) {
            return true;
        }
        if (response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
            logger.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
            return true;
        }
        boolean preFlightRequest = CorsUtils.isPreFlightRequest(request);
        if (config == null) {
            if (preFlightRequest) {
                rejectRequest(response);
                return false;
            } else {
                return true;
            }
        }
        return handleInternal(exchange, config, preFlightRequest);
    }

    protected void rejectRequest(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
    }

    protected boolean handleInternal(ServerWebExchange exchange, CorsConfiguration config, boolean preFlightRequest) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders responseHeaders = response.getHeaders();
        String requestOrigin = request.getHeaders().getOrigin();
        String allowOrigin = checkOrigin(config, requestOrigin);
        if (allowOrigin == null) {
            logger.debug("Reject: '" + requestOrigin + "' origin is not allowed");
            rejectRequest(response);
            return false;
        }
        HttpMethod requestMethod = getMethodToUse(request, preFlightRequest);
        List<HttpMethod> allowMethods = checkMethods(config, requestMethod);
        if (allowMethods == null) {
            logger.debug("Reject: HTTP '" + requestMethod + "' is not allowed");
            rejectRequest(response);
            return false;
        }
        List<String> requestHeaders = getHeadersToUse(request, preFlightRequest);
        List<String> allowHeaders = checkHeaders(config, requestHeaders);
        if (preFlightRequest && allowHeaders == null) {
            logger.debug("Reject: headers '" + requestHeaders + "' are not allowed");
            rejectRequest(response);
            return false;
        }
        responseHeaders.setAccessControlAllowOrigin(allowOrigin);
        if (preFlightRequest) {
            responseHeaders.setAccessControlAllowMethods(allowMethods);
        }
        if (preFlightRequest && !allowHeaders.isEmpty()) {
            responseHeaders.setAccessControlAllowHeaders(allowHeaders);
        }
        if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
            responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
        }
        if (Boolean.TRUE.equals(config.getAllowCredentials())) {
            responseHeaders.setAccessControlAllowCredentials(true);
        }
        if (preFlightRequest && config.getMaxAge() != null) {
            responseHeaders.setAccessControlMaxAge(config.getMaxAge());
        }
        return true;
    }

    @Nullable
    protected String checkOrigin(CorsConfiguration config, @Nullable String requestOrigin) {
        return config.checkOrigin(requestOrigin);
    }

    @Nullable
    protected List<HttpMethod> checkMethods(CorsConfiguration config, @Nullable HttpMethod requestMethod) {
        return config.checkHttpMethod(requestMethod);
    }

    @Nullable
    private HttpMethod getMethodToUse(ServerHttpRequest request, boolean isPreFlight) {
        return (isPreFlight ? request.getHeaders().getAccessControlRequestMethod() : request.getMethod());
    }

    @Nullable
    protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
        return config.checkHeaders(requestHeaders);
    }

    private List<String> getHeadersToUse(ServerHttpRequest request, boolean isPreFlight) {
        HttpHeaders headers = request.getHeaders();
        return (isPreFlight ? headers.getAccessControlRequestHeaders() : new ArrayList<>(headers.keySet()));
    }

}
