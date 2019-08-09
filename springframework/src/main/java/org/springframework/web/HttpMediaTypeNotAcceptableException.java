package org.springframework.web;

import org.springframework.http.MediaType;

import java.util.List;

@SuppressWarnings("serial")
public class HttpMediaTypeNotAcceptableException extends HttpMediaTypeException {

    public HttpMediaTypeNotAcceptableException(String message) {
        super(message);
    }

    public HttpMediaTypeNotAcceptableException(List<MediaType> supportedMediaTypes) {
        super("Could not find acceptable representation", supportedMediaTypes);
    }

}
