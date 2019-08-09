package org.springframework.http.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class MultipartBodyBuilder {

    private final LinkedMultiValueMap<String, DefaultPartBuilder> parts = new LinkedMultiValueMap<>();

    public MultipartBodyBuilder() {
    }

    public PartBuilder part(String name, Object part) {
        return part(name, part, null);
    }

    public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
        Assert.hasLength(name, "'name' must not be empty");
        Assert.notNull(part, "'part' must not be null");
        if (part instanceof Publisher) {
            throw new IllegalArgumentException("Use publisher(String, Publisher, Class) or " + "publisher(String, Publisher, ParameterizedTypeReference) for adding Publisher parts");
        }
        if (part instanceof PublisherEntity<?, ?>) {
            PublisherPartBuilder<?, ?> builder = new PublisherPartBuilder<>((PublisherEntity<?, ?>) part);
            this.parts.add(name, builder);
            return builder;
        }
        Object partBody;
        HttpHeaders partHeaders = new HttpHeaders();
        if (part instanceof HttpEntity) {
            HttpEntity<?> httpEntity = (HttpEntity<?>) part;
            partBody = httpEntity.getBody();
            partHeaders.addAll(httpEntity.getHeaders());
        } else {
            partBody = part;
        }
        if (contentType != null) {
            partHeaders.setContentType(contentType);
        }
        DefaultPartBuilder builder = new DefaultPartBuilder(partHeaders, partBody);
        this.parts.add(name, builder);
        return builder;
    }

    @SuppressWarnings("unchecked")
    public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher, Class<T> elementClass) {
        Assert.hasLength(name, "'name' must not be empty");
        Assert.notNull(publisher, "'publisher' must not be null");
        Assert.notNull(elementClass, "'elementClass' must not be null");
        if (Part.class.isAssignableFrom(elementClass)) {
            publisher = (P) Mono.from(publisher).flatMapMany(p -> ((Part) p).content());
            elementClass = (Class<T>) DataBuffer.class;
        }
        HttpHeaders headers = new HttpHeaders();
        PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(headers, publisher, elementClass);
        this.parts.add(name, builder);
        return builder;

    }

    public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher, ParameterizedTypeReference<T> typeReference) {
        Assert.hasLength(name, "'name' must not be empty");
        Assert.notNull(publisher, "'publisher' must not be null");
        Assert.notNull(typeReference, "'typeReference' must not be null");
        HttpHeaders headers = new HttpHeaders();
        PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(headers, publisher, typeReference);
        this.parts.add(name, builder);
        return builder;
    }

    public MultiValueMap<String, HttpEntity<?>> build() {
        MultiValueMap<String, HttpEntity<?>> result = new LinkedMultiValueMap<>(this.parts.size());
        for (Map.Entry<String, List<DefaultPartBuilder>> entry : this.parts.entrySet()) {
            for (DefaultPartBuilder builder : entry.getValue()) {
                HttpEntity<?> entity = builder.build();
                result.add(entry.getKey(), entity);
            }
        }
        return result;
    }

    public interface PartBuilder {

        PartBuilder header(String headerName, String... headerValues);

        PartBuilder headers(Consumer<HttpHeaders> headersConsumer);

    }

    private static class DefaultPartBuilder implements PartBuilder {

        protected final HttpHeaders headers;

        @Nullable
        protected final Object body;

        public DefaultPartBuilder(HttpHeaders headers, @Nullable Object body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public PartBuilder header(String headerName, String... headerValues) {
            this.headers.addAll(headerName, Arrays.asList(headerValues));
            return this;
        }

        @Override
        public PartBuilder headers(Consumer<HttpHeaders> headersConsumer) {
            headersConsumer.accept(this.headers);
            return this;
        }

        public HttpEntity<?> build() {
            return new HttpEntity<>(this.body, this.headers);
        }

    }

    private static class PublisherPartBuilder<S, P extends Publisher<S>> extends DefaultPartBuilder {

        private final ResolvableType resolvableType;

        public PublisherPartBuilder(HttpHeaders headers, P body, Class<S> elementClass) {
            super(headers, body);
            this.resolvableType = ResolvableType.forClass(elementClass);
        }

        public PublisherPartBuilder(HttpHeaders headers, P body, ParameterizedTypeReference<S> typeReference) {
            super(headers, body);
            this.resolvableType = ResolvableType.forType(typeReference);
        }

        public PublisherPartBuilder(PublisherEntity<S, P> other) {
            super(other.getHeaders(), other.getBody());
            this.resolvableType = other.getResolvableType();
        }

        @Override
        @SuppressWarnings("unchecked")
        public HttpEntity<?> build() {
            P publisher = (P) this.body;
            Assert.state(publisher != null, "Publisher must not be null");
            return new PublisherEntity<>(this.headers, publisher, this.resolvableType);
        }

    }

    public static final class PublisherEntity<T, P extends Publisher<T>> extends HttpEntity<P> implements ResolvableTypeProvider {

        private final ResolvableType resolvableType;

        PublisherEntity(@Nullable MultiValueMap<String, String> headers, P publisher, ResolvableType resolvableType) {
            super(publisher, headers);
            Assert.notNull(publisher, "'publisher' must not be null");
            Assert.notNull(resolvableType, "'resolvableType' must not be null");
            this.resolvableType = resolvableType;
        }

        @Override
        @NonNull
        public ResolvableType getResolvableType() {
            return this.resolvableType;
        }

    }

}
