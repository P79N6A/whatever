package org.springframework.web.filter.reactive;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HiddenHttpMethodFilter implements WebFilter {

    private static final List<HttpMethod> ALLOWED_METHODS = Collections.unmodifiableList(Arrays.asList(HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH));

    public static final String DEFAULT_METHOD_PARAMETER_NAME = "_method";

    private String methodParamName = DEFAULT_METHOD_PARAMETER_NAME;

    public void setMethodParamName(String methodParamName) {
        Assert.hasText(methodParamName, "'methodParamName' must not be empty");
        this.methodParamName = methodParamName;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod() != HttpMethod.POST) {
            return chain.filter(exchange);
        }
        return exchange.getFormData().map(formData -> {
            String method = formData.getFirst(this.methodParamName);
            return StringUtils.hasLength(method) ? mapExchange(exchange, method) : exchange;
        }).flatMap(chain::filter);
    }

    private ServerWebExchange mapExchange(ServerWebExchange exchange, String methodParamValue) {
        HttpMethod httpMethod = HttpMethod.resolve(methodParamValue.toUpperCase(Locale.ENGLISH));
        Assert.notNull(httpMethod, () -> "HttpMethod '" + methodParamValue + "' not supported");
        if (ALLOWED_METHODS.contains(httpMethod)) {
            return exchange.mutate().request(builder -> builder.method(httpMethod)).build();
        } else {
            return exchange;
        }
    }

}
