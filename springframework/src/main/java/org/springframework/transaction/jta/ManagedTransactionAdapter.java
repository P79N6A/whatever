package org.springframework.transaction.jta;

import org.springframework.util.Assert;

import javax.transaction.*;
import javax.transaction.xa.XAResource;

public class ManagedTransactionAdapter implements Transaction {

    private final TransactionManager transactionManager;

    public ManagedTransactionAdapter(TransactionManager transactionManager) throws SystemException {
        Assert.notNull(transactionManager, "TransactionManager must not be null");
        this.transactionManager = transactionManager;
    }

    public final TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        this.transactionManager.commit();
    }

    @Override
    public void rollback() throws SystemException {
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

    @Override
    public boolean enlistResource(XAResource xaRes) throws RollbackException, SystemException {
        return this.transactionManager.getTransaction().enlistResource(xaRes);
    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag) throws SystemException {
        return this.transactionManager.getTransaction().delistResource(xaRes, flag);
    }

    @Override
    public void registerSynchronization(Synchronization sync) throws RollbackException, SystemException {
        this.transactionManager.getTransaction().registerSynchronization(sync);
    }

}
