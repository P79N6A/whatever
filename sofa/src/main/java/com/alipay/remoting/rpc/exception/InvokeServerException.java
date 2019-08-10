package com.alipay.remoting.rpc.exception;

import com.alipay.remoting.exception.RemotingException;

public class InvokeServerException extends RemotingException {

    private static final long serialVersionUID = 4480283862377034355L;

    public InvokeServerException() {
    }

    public InvokeServerException(String msg) {
        super(msg);
    }

    public InvokeServerException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
