package org.springframework.transaction.jta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

public class SpringJtaSynchronizationAdapter implements Synchronization {

    protected static final Log logger = LogFactory.getLog(SpringJtaSynchronizationAdapter.class);

    private final TransactionSynchronization springSynchronization;

    @Nullable
    private UserTransaction jtaTransaction;

    private boolean beforeCompletionCalled = false;

    public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization) {
        Assert.notNull(springSynchronization, "TransactionSynchronization must not be null");
        this.springSynchronization = springSynchronization;
    }

    public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization, @Nullable UserTransaction jtaUserTransaction) {
        this(springSynchronization);
        if (jtaUserTransaction != null && !jtaUserTransaction.getClass().getName().startsWith("weblogic.")) {
            this.jtaTransaction = jtaUserTransaction;
        }
    }

    public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization, @Nullable TransactionManager jtaTransactionManager) {
        this(springSynchronization);
        if (jtaTransactionManager != null && !jtaTransactionManager.getClass().getName().startsWith("weblogic.")) {
            this.jtaTransaction = new UserTransactionAdapter(jtaTransactionManager);
        }
    }

    @Override
    public void beforeCompletion() {
        try {
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            this.springSynchronization.beforeCommit(readOnly);
        } catch (RuntimeException | Error ex) {
            setRollbackOnlyIfPossible();
            throw ex;
        } finally {
            // Process Spring's beforeCompletion early, in order to avoid issues
            // with strict JTA implementations that issue warnings when doing JDBC
            // operations after transaction completion (e.g. Connection.getWarnings).
            this.beforeCompletionCalled = true;
            this.springSynchronization.beforeCompletion();
        }
    }

    private void setRollbackOnlyIfPossible() {
        if (this.jtaTransaction != null) {
            try {
                this.jtaTransaction.setRollbackOnly();
            } catch (UnsupportedOperationException ex) {
                // Probably Hibernate's WebSphereExtendedJTATransactionLookup pseudo JTA stuff...
                logger.debug("JTA transaction handle does not support setRollbackOnly method - " + "relying on JTA provider to mark the transaction as rollback-only based on " + "the exception thrown from beforeCompletion", ex);
            } catch (Throwable ex) {
                logger.error("Could not set JTA transaction rollback-only", ex);
            }
        } else {
            logger.debug("No JTA transaction handle available and/or running on WebLogic - " + "relying on JTA provider to mark the transaction as rollback-only based on " + "the exception thrown from beforeCompletion");
        }
    }

    @Override
    public void afterCompletion(int status) {
        if (!this.beforeCompletionCalled) {
            // beforeCompletion not called before (probably because of JTA rollback).
            // Perform the cleanup here.
            this.springSynchronization.beforeCompletion();
        }
        // Call afterCompletion with the appropriate status indication.
        switch (status) {
            case Status.STATUS_COMMITTED:
                this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
                break;
            case Status.STATUS_ROLLEDBACK:
                this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
                break;
            default:
                this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
        }
    }

}
