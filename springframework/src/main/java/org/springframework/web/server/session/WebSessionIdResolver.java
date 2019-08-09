package org.springframework.web.server.session;

import org.springframework.web.server.ServerWebExchange;

import java.util.List;

public interface WebSessionIdResolver {

    List<String> resolveSessionIds(ServerWebExchange exchange);

    void setSessionId(ServerWebExchange exchange, String sessionId);

    void expireSession(ServerWebExchange exchange);

}
