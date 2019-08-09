package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;

public class ResponseEntity<T> extends HttpEntity<T> {

    private final Object status;

    public ResponseEntity(HttpStatus status) {
        this(null, null, status);
    }

    public ResponseEntity(@Nullable T body, HttpStatus status) {
        this(body, null, status);
    }

    public ResponseEntity(MultiValueMap<String, String> headers, HttpStatus status) {
        this(null, headers, status);
    }

    public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, HttpStatus status) {
        super(body, headers);
        Assert.notNull(status, "HttpStatus must not be null");
        this.status = status;
    }

    private ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, Object status) {
        super(body, headers);
        Assert.notNull(status, "HttpStatus must not be null");
        this.status = status;
    }

    public HttpStatus getStatusCode() {
        if (this.status instanceof HttpStatus) {
            return (HttpStatus) this.status;
        } else {
            return HttpStatus.valueOf((Integer) this.status);
        }
    }

    public int getStatusCodeValue() {
        if (this.status instanceof HttpStatus) {
            return ((HttpStatus) this.status).value();
        } else {
            return (Integer) this.status;
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
        return ObjectUtils.nullSafeEquals(this.status, otherEntity.status);
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.status));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("<");
        builder.append(this.status.toString());
        if (this.status instanceof HttpStatus) {
            builder.append(' ');
            builder.append(((HttpStatus) this.status).getReasonPhrase());
        }
        builder.append(',');
        T body = getBody();
        HttpHeaders headers = getHeaders();
        if (body != null) {
            builder.append(body);
            builder.append(',');
        }
        builder.append(headers);
        builder.append('>');
        return builder.toString();
    }
    // Static builder methods

    public static BodyBuilder status(HttpStatus status) {
        Assert.notNull(status, "HttpStatus must not be null");
        return new DefaultBuilder(status);
    }

    public static BodyBuilder status(int status) {
        return new DefaultBuilder(status);
    }

    public static <T> ResponseEntity<T> of(Optional<T> body) {
        Assert.notNull(body, "Body must not be null");
        return body.map(ResponseEntity::ok).orElse(notFound().build());
    }

    public static BodyBuilder ok() {
        return status(HttpStatus.OK);
    }

    public static <T> ResponseEntity<T> ok(T body) {
        BodyBuilder builder = ok();
        return builder.body(body);
    }

    public static BodyBuilder created(URI location) {
        BodyBuilder builder = status(HttpStatus.CREATED);
        return builder.location(location);
    }

    public static BodyBuilder accepted() {
        return status(HttpStatus.ACCEPTED);
    }

    public static HeadersBuilder<?> noContent() {
        return status(HttpStatus.NO_CONTENT);
    }

    public static BodyBuilder badRequest() {
        return status(HttpStatus.BAD_REQUEST);
    }

    public static HeadersBuilder<?> notFound() {
        return status(HttpStatus.NOT_FOUND);
    }

    public static BodyBuilder unprocessableEntity() {
        return status(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public interface HeadersBuilder<B extends HeadersBuilder<B>> {

        B header(String headerName, String... headerValues);

        B headers(@Nullable HttpHeaders headers);

        B allow(HttpMethod... allowedMethods);

        B eTag(String etag);

        B lastModified(ZonedDateTime lastModified);

        B lastModified(Instant lastModified);

        B lastModified(long lastModified);

        B location(URI location);

        B cacheControl(CacheControl cacheControl);

        B varyBy(String... requestHeaders);

        <T> ResponseEntity<T> build();

    }

    public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

        BodyBuilder contentLength(long contentLength);

        BodyBuilder contentType(MediaType contentType);

        <T> ResponseEntity<T> body(@Nullable T body);

    }

    private static class DefaultBuilder implements BodyBuilder {

        private final Object statusCode;

        private final HttpHeaders headers = new HttpHeaders();

        public DefaultBuilder(Object statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public BodyBuilder header(String headerName, String... headerValues) {
            for (String headerValue : headerValues) {
                this.headers.add(headerName, headerValue);
            }
            return this;
        }

        @Override
        public BodyBuilder headers(@Nullable HttpHeaders headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        @Override
        public BodyBuilder allow(HttpMethod... allowedMethods) {
            this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
            return this;
        }

        @Override
        public BodyBuilder contentLength(long contentLength) {
            this.headers.setContentLength(contentLength);
            return this;
        }

        @Override
        public BodyBuilder contentType(MediaType contentType) {
            this.headers.setContentType(contentType);
            return this;
        }

        @Override
        public BodyBuilder eTag(String etag) {
            if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
                etag = "\"" + etag;
            }
            if (!etag.endsWith("\"")) {
                etag = etag + "\"";
            }
            this.headers.setETag(etag);
            return this;
        }

        @Override
        public BodyBuilder lastModified(ZonedDateTime date) {
            this.headers.setLastModified(date);
            return this;
        }

        @Override
        public BodyBuilder lastModified(Instant date) {
            this.headers.setLastModified(date);
            return this;
        }

        @Override
        public BodyBuilder lastModified(long date) {
            this.headers.setLastModified(date);
            return this;
        }

        @Override
        public BodyBuilder location(URI location) {
            this.headers.setLocation(location);
            return this;
        }

        @Override
        public BodyBuilder cacheControl(CacheControl cacheControl) {
            this.headers.setCacheControl(cacheControl);
            return this;
        }

        @Override
        public BodyBuilder varyBy(String... requestHeaders) {
            this.headers.setVary(Arrays.asList(requestHeaders));
            return this;
        }

        @Override
        public <T> ResponseEntity<T> build() {
            return body(null);
        }

        @Override
        public <T> ResponseEntity<T> body(@Nullable T body) {
            return new ResponseEntity<>(body, this.headers, this.statusCode);
        }

    }

}
