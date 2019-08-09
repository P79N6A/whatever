package org.springframework.transaction.jta;

import org.springframework.util.Assert;

import javax.transaction.*;

public class UserTransactionAdapter implements UserTransaction {

    private final TransactionManager transactionManager;

    public UserTransactionAdapter(TransactionManager transactionManager) {
        Assert.notNull(transactionManager, "TransactionManager must not be null");
        this.transactionManager = transactionManager;
    }

    public final TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    @Override
    public void setTransactionTimeout(int timeout) throws SystemException {
        this.transactionManager.setTransactionTimeout(timeout);
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        this.transactionManager.begin();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        this.transactionManager.commit();
    }

    @Override
    public void rollback() throws SecurityException, SystemException {
        this.transactionManager.rollback();
    }

    @Override
    public void setRollbackOnly() throws SystemException {
        this.transactionManager.setRollbackOnly();
    }

    @Override
    public int getStatus() throws SystemException {
        return this.transactionManager.getStatus();
    }

}
