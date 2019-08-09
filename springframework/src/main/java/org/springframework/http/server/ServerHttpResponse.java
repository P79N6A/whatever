package org.springframework.http.server;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

    void setStatusCode(HttpStatus status);

    @Override
    void flush() throws IOException;

    @Override
    void close();

}
