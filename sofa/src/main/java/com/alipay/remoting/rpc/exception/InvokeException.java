package com.alipay.remoting.rpc.exception;

import com.alipay.remoting.exception.RemotingException;

public class InvokeException extends RemotingException {

    private static final long serialVersionUID = -3974514863386363570L;

    public InvokeException() {
    }

    public InvokeException(String msg) {
        super(msg);
    }

    public InvokeException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
