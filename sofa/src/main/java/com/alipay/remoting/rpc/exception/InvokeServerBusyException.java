package com.alipay.remoting.rpc.exception;

import com.alipay.remoting.exception.RemotingException;

public class InvokeServerBusyException extends RemotingException {

    private static final long serialVersionUID = 4480283862377034355L;

    public InvokeServerBusyException() {
    }

    public InvokeServerBusyException(String msg) {
        super(msg);
    }

    public InvokeServerBusyException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
