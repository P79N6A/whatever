package org.springframework.http.client;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;

import java.io.Closeable;
import java.io.IOException;

public interface ClientHttpResponse extends HttpInputMessage, Closeable {

    HttpStatus getStatusCode() throws IOException;

    int getRawStatusCode() throws IOException;

    String getStatusText() throws IOException;

    @Override
    void close();

}
