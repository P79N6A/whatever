package com.alipay.remoting.rpc.exception;

import com.alipay.remoting.exception.RemotingException;

public class InvokeSendFailedException extends RemotingException {

    private static final long serialVersionUID = 4832257777758730796L;

    public InvokeSendFailedException() {
    }

    public InvokeSendFailedException(String msg) {
        super(msg);
    }

    public InvokeSendFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
