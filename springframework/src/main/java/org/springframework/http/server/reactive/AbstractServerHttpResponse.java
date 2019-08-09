package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

    private enum State {NEW, COMMITTING, COMMITTED}

    protected final Log logger = HttpLogging.forLogName(getClass());

    private final DataBufferFactory dataBufferFactory;

    @Nullable
    private Integer statusCode;

    private final HttpHeaders headers;

    private final MultiValueMap<String, ResponseCookie> cookies;

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    private final List<Supplier<? extends Mono<Void>>> commitActions = new ArrayList<>(4);

    public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory) {
        this(dataBufferFactory, new HttpHeaders());
    }

    public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory, HttpHeaders headers) {
        Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
        Assert.notNull(headers, "HttpHeaders must not be null");
        this.dataBufferFactory = dataBufferFactory;
        this.headers = headers;
        this.cookies = new LinkedMultiValueMap<>();
    }

    @Override
    public final DataBufferFactory bufferFactory() {
        return this.dataBufferFactory;
    }

    @Override
    public boolean setStatusCode(@Nullable HttpStatus status) {
        if (this.state.get() == State.COMMITTED) {
            return false;
        } else {
            this.statusCode = (status != null ? status.value() : null);
            return true;
        }
    }

    @Override
    @Nullable
    public HttpStatus getStatusCode() {
        return this.statusCode != null ? HttpStatus.resolve(this.statusCode) : null;
    }

    public void setStatusCodeValue(@Nullable Integer statusCode) {
        this.statusCode = statusCode;
    }

    @Nullable
    public Integer getStatusCodeValue() {
        return this.statusCode;
    }

    @Override
    public HttpHeaders getHeaders() {
        return (this.state.get() == State.COMMITTED ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
    }

    @Override
    public MultiValueMap<String, ResponseCookie> getCookies() {
        return (this.state.get() == State.COMMITTED ? CollectionUtils.unmodifiableMultiValueMap(this.cookies) : this.cookies);
    }

    @Override
    public void addCookie(ResponseCookie cookie) {
        Assert.notNull(cookie, "ResponseCookie must not be null");
        if (this.state.get() == State.COMMITTED) {
            throw new IllegalStateException("Can't add the cookie " + cookie + "because the HTTP response has already been committed");
        } else {
            getCookies().add(cookie.getName(), cookie);
        }
    }

    public abstract <T> T getNativeResponse();

    @Override
    public void beforeCommit(Supplier<? extends Mono<Void>> action) {
        this.commitActions.add(action);
    }

    @Override
    public boolean isCommitted() {
        return this.state.get() != State.NEW;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        // Write as Mono if possible as an optimization hint to Reactor Netty
        // ChannelSendOperator not necessary for Mono
        if (body instanceof Mono) {
            return ((Mono<? extends DataBuffer>) body).flatMap(buffer -> doCommit(() -> writeWithInternal(Mono.just(buffer))).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
        }
        return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeWithInternal(inner))).doOnError(t -> removeContentLength());
    }

    @Override
    public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeAndFlushWithInternal(inner))).doOnError(t -> removeContentLength());
    }

    private void removeContentLength() {
        if (!this.isCommitted()) {
            this.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
        }
    }

    @Override
    public Mono<Void> setComplete() {
        return !isCommitted() ? doCommit(null) : Mono.empty();
    }

    protected Mono<Void> doCommit() {
        return doCommit(null);
    }

    protected Mono<Void> doCommit(@Nullable Supplier<? extends Mono<Void>> writeAction) {
        if (!this.state.compareAndSet(State.NEW, State.COMMITTING)) {
            return Mono.empty();
        }
        this.commitActions.add(() -> Mono.fromRunnable(() -> {
            applyStatusCode();
            applyHeaders();
            applyCookies();
            this.state.set(State.COMMITTED);
        }));
        if (writeAction != null) {
            this.commitActions.add(writeAction);
        }
        Flux<Void> commit = Flux.empty();
        for (Supplier<? extends Mono<Void>> action : this.commitActions) {
            commit = commit.concatWith(action.get());
        }
        return commit.then();
    }

    protected abstract Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body);

    protected abstract Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body);

    protected abstract void applyStatusCode();

    protected abstract void applyHeaders();

    protected abstract void applyCookies();

}
