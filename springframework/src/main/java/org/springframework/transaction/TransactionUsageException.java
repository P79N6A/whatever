package org.springframework.transaction;

@SuppressWarnings("serial")
public class TransactionUsageException extends TransactionException {

    public TransactionUsageException(String msg) {
        super(msg);
    }

    public TransactionUsageException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
