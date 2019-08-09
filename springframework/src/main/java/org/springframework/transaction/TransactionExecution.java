package org.springframework.transaction;

public interface TransactionExecution {

    boolean isNewTransaction();

    void setRollbackOnly();

    boolean isRollbackOnly();

    boolean isCompleted();

}
