package org.springframework.http.codec.multipart;

import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;

public interface FilePart extends Part {

    String filename();

    default Mono<Void> transferTo(File dest) {
        return transferTo(dest.toPath());
    }

    Mono<Void> transferTo(Path dest);

}
