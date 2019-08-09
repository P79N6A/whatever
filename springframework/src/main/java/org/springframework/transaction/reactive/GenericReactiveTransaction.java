package org.springframework.transaction.reactive;

import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.util.Assert;

public class GenericReactiveTransaction implements ReactiveTransaction {

    @Nullable
    private final Object transaction;

    private final boolean newTransaction;

    private final boolean newSynchronization;

    private final boolean readOnly;

    private final boolean debug;

    @Nullable
    private final Object suspendedResources;

    private boolean rollbackOnly = false;

    private boolean completed = false;

    public GenericReactiveTransaction(@Nullable Object transaction, boolean newTransaction, boolean newSynchronization, boolean readOnly, boolean debug, @Nullable Object suspendedResources) {
        this.transaction = transaction;
        this.newTransaction = newTransaction;
        this.newSynchronization = newSynchronization;
        this.readOnly = readOnly;
        this.debug = debug;
        this.suspendedResources = suspendedResources;
    }

    public Object getTransaction() {
        Assert.state(this.transaction != null, "No transaction active");
        return this.transaction;
    }

    public boolean hasTransaction() {
        return (this.transaction != null);
    }

    @Override
    public boolean isNewTransaction() {
        return (hasTransaction() && this.newTransaction);
    }

    public boolean isNewSynchronization() {
        return this.newSynchronization;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public boolean isDebug() {
        return this.debug;
    }

    @Nullable
    public Object getSuspendedResources() {
        return this.suspendedResources;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean isRollbackOnly() {
        return this.rollbackOnly;
    }

    public void setCompleted() {
        this.completed = true;
    }

    @Override
    public boolean isCompleted() {
        return this.completed;
    }

}
