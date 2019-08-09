package org.springframework.web.server.session;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class DefaultWebSessionManager implements WebSessionManager {

    private WebSessionIdResolver sessionIdResolver = new CookieWebSessionIdResolver();

    private WebSessionStore sessionStore = new InMemoryWebSessionStore();

    public void setSessionIdResolver(WebSessionIdResolver sessionIdResolver) {
        Assert.notNull(sessionIdResolver, "WebSessionIdResolver is required");
        this.sessionIdResolver = sessionIdResolver;
    }

    public WebSessionIdResolver getSessionIdResolver() {
        return this.sessionIdResolver;
    }

    public void setSessionStore(WebSessionStore sessionStore) {
        Assert.notNull(sessionStore, "WebSessionStore is required");
        this.sessionStore = sessionStore;
    }

    public WebSessionStore getSessionStore() {
        return this.sessionStore;
    }

    @Override
    public Mono<WebSession> getSession(ServerWebExchange exchange) {
        return Mono.defer(() -> retrieveSession(exchange).switchIfEmpty(this.sessionStore.createWebSession()).doOnNext(session -> exchange.getResponse().beforeCommit(() -> save(exchange, session))));
    }

    private Mono<WebSession> retrieveSession(ServerWebExchange exchange) {
        return Flux.fromIterable(getSessionIdResolver().resolveSessionIds(exchange)).concatMap(this.sessionStore::retrieveSession).next();
    }

    private Mono<Void> save(ServerWebExchange exchange, WebSession session) {
        List<String> ids = getSessionIdResolver().resolveSessionIds(exchange);
        if (!session.isStarted() || session.isExpired()) {
            if (!ids.isEmpty()) {
                // Expired on retrieve or while processing request, or invalidated..
                this.sessionIdResolver.expireSession(exchange);
            }
            return Mono.empty();
        }
        if (ids.isEmpty() || !session.getId().equals(ids.get(0))) {
            this.sessionIdResolver.setSessionId(exchange, session.getId());
        }
        return session.save();
    }

}
