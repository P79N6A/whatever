package org.aopalliance.aop;

@SuppressWarnings("serial")
public class AspectException extends RuntimeException {

    public AspectException(String message) {
        super(message);
    }

    public AspectException(String message, Throwable cause) {
        super(message, cause);
    }

}
