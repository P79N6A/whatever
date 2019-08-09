package org.springframework.web.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ServerWebExchange {

    String LOG_ID_ATTRIBUTE = ServerWebExchange.class.getName() + ".LOG_ID";

    ServerHttpRequest getRequest();

    ServerHttpResponse getResponse();

    Map<String, Object> getAttributes();

    @SuppressWarnings("unchecked")
    @Nullable
    default <T> T getAttribute(String name) {
        return (T) getAttributes().get(name);
    }

    @SuppressWarnings("unchecked")
    default <T> T getRequiredAttribute(String name) {
        T value = getAttribute(name);
        Assert.notNull(value, () -> "Required attribute '" + name + "' is missing");
        return value;
    }

    @SuppressWarnings("unchecked")
    default <T> T getAttributeOrDefault(String name, T defaultValue) {
        return (T) getAttributes().getOrDefault(name, defaultValue);
    }

    Mono<WebSession> getSession();

    <T extends Principal> Mono<T> getPrincipal();

    Mono<MultiValueMap<String, String>> getFormData();

    Mono<MultiValueMap<String, Part>> getMultipartData();

    LocaleContext getLocaleContext();

    @Nullable
    ApplicationContext getApplicationContext();

    boolean isNotModified();

    boolean checkNotModified(Instant lastModified);

    boolean checkNotModified(String etag);

    boolean checkNotModified(@Nullable String etag, Instant lastModified);

    String transformUrl(String url);

    void addUrlTransformer(Function<String, String> transformer);

    String getLogPrefix();

    default Builder mutate() {
        return new DefaultServerWebExchangeBuilder(this);
    }

    interface Builder {

        Builder request(Consumer<ServerHttpRequest.Builder> requestBuilderConsumer);

        Builder request(ServerHttpRequest request);

        Builder response(ServerHttpResponse response);

        Builder principal(Mono<Principal> principalMono);

        ServerWebExchange build();

    }

}
