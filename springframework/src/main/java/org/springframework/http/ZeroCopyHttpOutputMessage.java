package org.springframework.http;

import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;

public interface ZeroCopyHttpOutputMessage extends ReactiveHttpOutputMessage {

    default Mono<Void> writeWith(File file, long position, long count) {
        return writeWith(file.toPath(), position, count);
    }

    Mono<Void> writeWith(Path file, long position, long count);

}
