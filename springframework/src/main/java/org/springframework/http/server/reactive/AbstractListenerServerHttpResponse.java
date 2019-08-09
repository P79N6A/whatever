package org.springframework.http.server.reactive;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractListenerServerHttpResponse extends AbstractServerHttpResponse {

    private final AtomicBoolean writeCalled = new AtomicBoolean();

    public AbstractListenerServerHttpResponse(DataBufferFactory dataBufferFactory) {
        super(dataBufferFactory);
    }

    public AbstractListenerServerHttpResponse(DataBufferFactory dataBufferFactory, HttpHeaders headers) {
        super(dataBufferFactory, headers);
    }

    @Override
    protected final Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
        return writeAndFlushWithInternal(Mono.just(body));
    }

    @Override
    protected final Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        if (this.writeCalled.compareAndSet(false, true)) {
            Processor<? super Publisher<? extends DataBuffer>, Void> processor = createBodyFlushProcessor();
            return Mono.from(subscriber -> {
                body.subscribe(processor);
                processor.subscribe(subscriber);
            });
        }
        return Mono.error(new IllegalStateException("writeWith() or writeAndFlushWith() has already been called"));
    }

    protected abstract Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor();

}
