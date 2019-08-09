package org.springframework.expression;

@SuppressWarnings("serial")
public class AccessException extends Exception {

    public AccessException(String message) {
        super(message);
    }

    public AccessException(String message, Exception cause) {
        super(message, cause);
    }

}
