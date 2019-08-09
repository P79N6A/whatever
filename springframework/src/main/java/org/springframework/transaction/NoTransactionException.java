package org.springframework.transaction;

@SuppressWarnings("serial")
public class NoTransactionException extends TransactionUsageException {

    public NoTransactionException(String msg) {
        super(msg);
    }

    public NoTransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
