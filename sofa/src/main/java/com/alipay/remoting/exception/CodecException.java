package com.alipay.remoting.exception;

public class CodecException extends RemotingException {

    private static final long serialVersionUID = -7513762648815278960L;

    public CodecException() {
    }

    public CodecException(String message) {
        super(message);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }

}