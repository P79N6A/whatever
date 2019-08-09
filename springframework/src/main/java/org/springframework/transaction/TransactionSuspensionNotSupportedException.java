package org.springframework.transaction;

@SuppressWarnings("serial")
public class TransactionSuspensionNotSupportedException extends CannotCreateTransactionException {

    public TransactionSuspensionNotSupportedException(String msg) {
        super(msg);
    }

    public TransactionSuspensionNotSupportedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
