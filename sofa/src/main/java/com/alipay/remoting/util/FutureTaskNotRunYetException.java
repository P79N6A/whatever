package com.alipay.remoting.util;

public class FutureTaskNotRunYetException extends Exception {

    private static final long serialVersionUID = 2929126204324060632L;

    public FutureTaskNotRunYetException() {
    }

    public FutureTaskNotRunYetException(String message) {
        super(message);
    }

    public FutureTaskNotRunYetException(String message, Throwable cause) {
        super(message, cause);
    }

}