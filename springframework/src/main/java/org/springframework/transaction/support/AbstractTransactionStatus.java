package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.*;

public abstract class AbstractTransactionStatus implements TransactionStatus {

    private boolean rollbackOnly = false;

    private boolean completed = false;

    @Nullable
    private Object savepoint;
    //---------------------------------------------------------------------
    // Implementation of TransactionExecution
    //---------------------------------------------------------------------

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean isRollbackOnly() {
        return (isLocalRollbackOnly() || isGlobalRollbackOnly());
    }

    public boolean isLocalRollbackOnly() {
        return this.rollbackOnly;
    }

    public boolean isGlobalRollbackOnly() {
        return false;
    }

    public void setCompleted() {
        this.completed = true;
    }

    @Override
    public boolean isCompleted() {
        return this.completed;
    }
    //---------------------------------------------------------------------
    // Handling of current savepoint state
    //---------------------------------------------------------------------

    @Override
    public boolean hasSavepoint() {
        return (this.savepoint != null);
    }

    protected void setSavepoint(@Nullable Object savepoint) {
        this.savepoint = savepoint;
    }

    @Nullable
    protected Object getSavepoint() {
        return this.savepoint;
    }

    public void createAndHoldSavepoint() throws TransactionException {
        setSavepoint(getSavepointManager().createSavepoint());
    }

    public void rollbackToHeldSavepoint() throws TransactionException {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionUsageException("Cannot roll back to savepoint - no savepoint associated with current transaction");
        }
        getSavepointManager().rollbackToSavepoint(savepoint);
        getSavepointManager().releaseSavepoint(savepoint);
        setSavepoint(null);
    }

    public void releaseHeldSavepoint() throws TransactionException {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionUsageException("Cannot release savepoint - no savepoint associated with current transaction");
        }
        getSavepointManager().releaseSavepoint(savepoint);
        setSavepoint(null);
    }
    //---------------------------------------------------------------------
    // Implementation of SavepointManager
    //---------------------------------------------------------------------

    @Override
    public Object createSavepoint() throws TransactionException {
        return getSavepointManager().createSavepoint();
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().rollbackToSavepoint(savepoint);
    }

    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().releaseSavepoint(savepoint);
    }

    protected SavepointManager getSavepointManager() {
        throw new NestedTransactionNotSupportedException("This transaction does not support savepoints");
    }
    //---------------------------------------------------------------------
    // Flushing support
    //---------------------------------------------------------------------

    @Override
    public void flush() {
    }

}
