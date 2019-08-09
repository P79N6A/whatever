package org.springframework.boot.web.server;

@SuppressWarnings("serial")
public class WebServerException extends RuntimeException {

    public WebServerException(String message, Throwable cause) {
        super(message, cause);
    }

}
