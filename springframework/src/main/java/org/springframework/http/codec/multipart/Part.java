package org.springframework.http.codec.multipart;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;

public interface Part {

    String name();

    HttpHeaders headers();

    Flux<DataBuffer> content();

}
