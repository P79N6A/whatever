package com.alipay.remoting.rpc.exception;

import com.alipay.remoting.exception.RemotingException;

public class InvokeTimeoutException extends RemotingException {

    private static final long serialVersionUID = -7772633244795043476L;

    public InvokeTimeoutException() {
    }

    public InvokeTimeoutException(String msg) {
        super(msg);
    }

    public InvokeTimeoutException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
