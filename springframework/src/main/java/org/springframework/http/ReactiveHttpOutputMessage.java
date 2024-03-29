package org.springframework.http;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface ReactiveHttpOutputMessage extends HttpMessage {

    DataBufferFactory bufferFactory();

    void beforeCommit(Supplier<? extends Mono<Void>> action);

    boolean isCommitted();

    Mono<Void> writeWith(Publisher<? extends DataBuffer> body);

    Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body);

    Mono<Void> setComplete();

}
