package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HttpHeadResponseDecorator extends ServerHttpResponseDecorator {

    public HttpHeadResponseDecorator(ServerHttpResponse delegate) {
        super(delegate);
    }

    @Override
    public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        return Flux.from(body).reduce(0, (current, buffer) -> {
            int next = current + buffer.readableByteCount();
            DataBufferUtils.release(buffer);
            return next;
        }).doOnNext(count -> getHeaders().setContentLength(count)).then();
    }

    @Override
    public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        // Not feasible to count bytes on potentially streaming response.
        // RFC 7302 allows HEAD without content-length.
        return setComplete();
    }

}
