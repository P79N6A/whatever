package com.alipay.remoting.exception;

public class ConnectionClosedException extends RemotingException {

    private static final long serialVersionUID = -2595820033346329315L;

    public ConnectionClosedException() {
    }

    public ConnectionClosedException(String msg) {
        super(msg);
    }

    public ConnectionClosedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
