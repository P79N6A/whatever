package org.springframework.web.client;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;

import java.io.IOException;

@FunctionalInterface
public interface ResponseExtractor<T> {

    @Nullable
    T extractData(ClientHttpResponse response) throws IOException;

}
