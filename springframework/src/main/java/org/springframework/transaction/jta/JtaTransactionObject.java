package org.springframework.transaction.jta;

import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

public class JtaTransactionObject implements SmartTransactionObject {

    private final UserTransaction userTransaction;

    boolean resetTransactionTimeout = false;

    public JtaTransactionObject(UserTransaction userTransaction) {
        this.userTransaction = userTransaction;
    }

    public final UserTransaction getUserTransaction() {
        return this.userTransaction;
    }

    @Override
    public boolean isRollbackOnly() {
        try {
            int jtaStatus = this.userTransaction.getStatus();
            return (jtaStatus == Status.STATUS_MARKED_ROLLBACK || jtaStatus == Status.STATUS_ROLLEDBACK);
        } catch (SystemException ex) {
            throw new TransactionSystemException("JTA failure on getStatus", ex);
        }
    }

    @Override
    public void flush() {
        TransactionSynchronizationUtils.triggerFlush();
    }

}
