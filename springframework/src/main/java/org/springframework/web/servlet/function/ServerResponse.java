package org.springframework.web.servlet.function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface ServerResponse {

    HttpStatus statusCode();

    HttpHeaders headers();

    MultiValueMap<String, Cookie> cookies();

    @Nullable
    ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context) throws ServletException, IOException;
    // Static methods

    static BodyBuilder from(ServerResponse other) {
        return new DefaultServerResponseBuilder(other);
    }

    static BodyBuilder status(HttpStatus status) {
        return new DefaultServerResponseBuilder(status);
    }

    static BodyBuilder status(int status) {
        return new DefaultServerResponseBuilder(status);
    }

    static BodyBuilder ok() {
        return status(HttpStatus.OK);
    }

    static BodyBuilder created(URI location) {
        BodyBuilder builder = status(HttpStatus.CREATED);
        return builder.location(location);
    }

    static BodyBuilder accepted() {
        return status(HttpStatus.ACCEPTED);
    }

    static HeadersBuilder<?> noContent() {
        return status(HttpStatus.NO_CONTENT);
    }

    static BodyBuilder seeOther(URI location) {
        BodyBuilder builder = status(HttpStatus.SEE_OTHER);
        return builder.location(location);
    }

    static BodyBuilder temporaryRedirect(URI location) {
        BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
        return builder.location(location);
    }

    static BodyBuilder permanentRedirect(URI location) {
        BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
        return builder.location(location);
    }

    static BodyBuilder badRequest() {
        return status(HttpStatus.BAD_REQUEST);
    }

    static HeadersBuilder<?> notFound() {
        return status(HttpStatus.NOT_FOUND);
    }

    static BodyBuilder unprocessableEntity() {
        return status(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    interface HeadersBuilder<B extends HeadersBuilder<B>> {

        B header(String headerName, String... headerValues);

        B headers(Consumer<HttpHeaders> headersConsumer);

        B cookie(Cookie cookie);

        B cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

        B allow(HttpMethod... allowedMethods);

        B allow(Set<HttpMethod> allowedMethods);

        B eTag(String eTag);

        B lastModified(ZonedDateTime lastModified);

        B lastModified(Instant lastModified);

        B location(URI location);

        B cacheControl(CacheControl cacheControl);

        B varyBy(String... requestHeaders);

        ServerResponse build();

        ServerResponse build(BiFunction<HttpServletRequest, HttpServletResponse, ModelAndView> writeFunction);

    }

    interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

        BodyBuilder contentLength(long contentLength);

        BodyBuilder contentType(MediaType contentType);

        ServerResponse body(Object body);

        <T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType);

        ServerResponse render(String name, Object... modelAttributes);

        ServerResponse render(String name, Map<String, ?> model);

    }

    interface Context {

        List<HttpMessageConverter<?>> messageConverters();

    }

}
