package com.alipay.remoting.exception;

public class RemotingException extends Exception {

    private static final long serialVersionUID = 6183635628271812505L;

    public RemotingException() {
    }

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }

}
