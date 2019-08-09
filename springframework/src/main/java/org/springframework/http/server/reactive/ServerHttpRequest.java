package org.springframework.http.server.reactive;

import org.springframework.http.*;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;

public interface ServerHttpRequest extends HttpRequest, ReactiveHttpInputMessage {

    String getId();

    RequestPath getPath();

    MultiValueMap<String, String> getQueryParams();

    MultiValueMap<String, HttpCookie> getCookies();

    @Nullable
    default InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Nullable
    default SslInfo getSslInfo() {
        return null;
    }

    default ServerHttpRequest.Builder mutate() {
        return new DefaultServerHttpRequestBuilder(this);
    }

    interface Builder {

        Builder method(HttpMethod httpMethod);

        Builder uri(URI uri);

        Builder path(String path);

        Builder contextPath(String contextPath);

        Builder header(String key, String value);

        Builder headers(Consumer<HttpHeaders> headersConsumer);

        Builder sslInfo(SslInfo sslInfo);

        ServerHttpRequest build();

    }

}
