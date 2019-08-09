package org.springframework.web.servlet.function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;
import java.util.function.Consumer;

public interface ServerRequest {

    @Nullable
    default HttpMethod method() {
        return HttpMethod.resolve(methodName());
    }

    String methodName();

    URI uri();

    UriBuilder uriBuilder();

    default String path() {
        return uri().getRawPath();
    }

    default PathContainer pathContainer() {
        return PathContainer.parsePath(path());
    }

    Headers headers();

    MultiValueMap<String, Cookie> cookies();

    Optional<InetSocketAddress> remoteAddress();

    List<HttpMessageConverter<?>> messageConverters();

    <T> T body(Class<T> bodyType) throws ServletException, IOException;

    <T> T body(ParameterizedTypeReference<T> bodyType) throws ServletException, IOException;

    default Optional<Object> attribute(String name) {
        Map<String, Object> attributes = attributes();
        if (attributes.containsKey(name)) {
            return Optional.of(attributes.get(name));
        } else {
            return Optional.empty();
        }
    }

    Map<String, Object> attributes();

    default Optional<String> param(String name) {
        List<String> paramValues = params().get(name);
        if (CollectionUtils.isEmpty(paramValues)) {
            return Optional.empty();
        } else {
            String value = paramValues.get(0);
            if (value == null) {
                value = "";
            }
            return Optional.of(value);
        }
    }

    MultiValueMap<String, String> params();

    default String pathVariable(String name) {
        Map<String, String> pathVariables = pathVariables();
        if (pathVariables.containsKey(name)) {
            return pathVariables().get(name);
        } else {
            throw new IllegalArgumentException("No path variable with name \"" + name + "\" available");
        }
    }

    Map<String, String> pathVariables();

    HttpSession session();

    Optional<Principal> principal();

    HttpServletRequest servletRequest();
    // Static methods

    static ServerRequest create(HttpServletRequest servletRequest, List<HttpMessageConverter<?>> messageReaders) {
        return new DefaultServerRequest(servletRequest, messageReaders);
    }

    static Builder from(ServerRequest other) {
        return new DefaultServerRequestBuilder(other);
    }

    interface Headers {

        List<MediaType> accept();

        List<Charset> acceptCharset();

        List<Locale.LanguageRange> acceptLanguage();

        OptionalLong contentLength();

        Optional<MediaType> contentType();

        @Nullable
        InetSocketAddress host();

        List<HttpRange> range();

        List<String> header(String headerName);

        HttpHeaders asHttpHeaders();

    }

    interface Builder {

        Builder method(HttpMethod method);

        Builder uri(URI uri);

        Builder header(String headerName, String... headerValues);

        Builder headers(Consumer<HttpHeaders> headersConsumer);

        Builder cookie(String name, String... values);

        Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

        Builder body(byte[] body);

        Builder body(String body);

        Builder attribute(String name, Object value);

        Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

        ServerRequest build();

    }

}
