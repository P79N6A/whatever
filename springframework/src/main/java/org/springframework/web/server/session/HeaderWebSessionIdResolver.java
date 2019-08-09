package org.springframework.web.server.session;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.List;

public class HeaderWebSessionIdResolver implements WebSessionIdResolver {

    public static final String DEFAULT_HEADER_NAME = "SESSION";

    private String headerName = DEFAULT_HEADER_NAME;

    public void setHeaderName(String headerName) {
        Assert.hasText(headerName, "'headerName' must not be empty");
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return this.headerName;
    }

    @Override
    public List<String> resolveSessionIds(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return headers.getOrDefault(getHeaderName(), Collections.emptyList());
    }

    @Override
    public void setSessionId(ServerWebExchange exchange, String id) {
        Assert.notNull(id, "'id' is required.");
        exchange.getResponse().getHeaders().set(getHeaderName(), id);
    }

    @Override
    public void expireSession(ServerWebExchange exchange) {
        this.setSessionId(exchange, "");
    }

}
