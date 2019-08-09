package org.springframework.http.converter.json;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class MappingJacksonInputMessage implements HttpInputMessage {

    private final InputStream body;

    private final HttpHeaders headers;

    @Nullable
    private Class<?> deserializationView;

    public MappingJacksonInputMessage(InputStream body, HttpHeaders headers) {
        this.body = body;
        this.headers = headers;
    }

    public MappingJacksonInputMessage(InputStream body, HttpHeaders headers, Class<?> deserializationView) {
        this(body, headers);
        this.deserializationView = deserializationView;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.body;
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }

    public void setDeserializationView(@Nullable Class<?> deserializationView) {
        this.deserializationView = deserializationView;
    }

    @Nullable
    public Class<?> getDeserializationView() {
        return this.deserializationView;
    }

}
