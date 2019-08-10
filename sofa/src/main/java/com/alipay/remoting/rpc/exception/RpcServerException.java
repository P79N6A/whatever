package com.alipay.remoting.rpc.exception;

import com.alipay.remoting.exception.RemotingException;

public class RpcServerException extends RemotingException {

    private static final long serialVersionUID = 4480283862377034355L;

    public RpcServerException() {
    }

    public RpcServerException(String msg) {
        super(msg);
    }

    public RpcServerException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
