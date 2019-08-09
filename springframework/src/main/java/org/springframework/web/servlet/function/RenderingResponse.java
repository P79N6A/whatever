package org.springframework.web.servlet.function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.Cookie;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public interface RenderingResponse extends ServerResponse {

    String name();

    Map<String, Object> model();
    // Builder

    static Builder from(RenderingResponse other) {
        return new DefaultRenderingResponseBuilder(other);
    }

    static Builder create(String name) {
        return new DefaultRenderingResponseBuilder(name);
    }

    interface Builder {

        Builder modelAttribute(Object attribute);

        Builder modelAttribute(String name, @Nullable Object value);

        Builder modelAttributes(Object... attributes);

        Builder modelAttributes(Collection<?> attributes);

        Builder modelAttributes(Map<String, ?> attributes);

        Builder header(String headerName, String... headerValues);

        Builder headers(Consumer<HttpHeaders> headersConsumer);

        Builder status(HttpStatus status);

        Builder status(int status);

        Builder cookie(Cookie cookie);

        Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

        RenderingResponse build();

    }

}
