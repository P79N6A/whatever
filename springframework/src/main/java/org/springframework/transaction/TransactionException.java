package org.springframework.transaction;

import org.springframework.core.NestedRuntimeException;

@SuppressWarnings("serial")
public abstract class TransactionException extends NestedRuntimeException {

    public TransactionException(String msg) {
        super(msg);
    }

    public TransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
