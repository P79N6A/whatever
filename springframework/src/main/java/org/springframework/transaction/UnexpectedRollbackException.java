package org.springframework.transaction;

@SuppressWarnings("serial")
public class UnexpectedRollbackException extends TransactionException {

    public UnexpectedRollbackException(String msg) {
        super(msg);
    }

    public UnexpectedRollbackException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
