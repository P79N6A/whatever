package org.springframework.web;

import org.springframework.http.MediaType;

import javax.servlet.ServletException;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public abstract class HttpMediaTypeException extends ServletException {

    private final List<MediaType> supportedMediaTypes;

    protected HttpMediaTypeException(String message) {
        super(message);
        this.supportedMediaTypes = Collections.emptyList();
    }

    protected HttpMediaTypeException(String message, List<MediaType> supportedMediaTypes) {
        super(message);
        this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
    }

    public List<MediaType> getSupportedMediaTypes() {
        return this.supportedMediaTypes;
    }

}
