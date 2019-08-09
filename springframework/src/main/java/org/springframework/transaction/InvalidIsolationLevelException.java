package org.springframework.transaction;

@SuppressWarnings("serial")
public class InvalidIsolationLevelException extends TransactionUsageException {

    public InvalidIsolationLevelException(String msg) {
        super(msg);
    }

}
