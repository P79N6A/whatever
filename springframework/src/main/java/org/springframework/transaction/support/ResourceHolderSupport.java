package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionTimedOutException;

import java.util.Date;

public abstract class ResourceHolderSupport implements ResourceHolder {

    private boolean synchronizedWithTransaction = false;

    private boolean rollbackOnly = false;

    @Nullable
    private Date deadline;

    private int referenceCount = 0;

    private boolean isVoid = false;

    public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
        this.synchronizedWithTransaction = synchronizedWithTransaction;
    }

    public boolean isSynchronizedWithTransaction() {
        return this.synchronizedWithTransaction;
    }

    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    public void resetRollbackOnly() {
        this.rollbackOnly = false;
    }

    public boolean isRollbackOnly() {
        return this.rollbackOnly;
    }

    public void setTimeoutInSeconds(int seconds) {
        setTimeoutInMillis(seconds * 1000L);
    }

    public void setTimeoutInMillis(long millis) {
        this.deadline = new Date(System.currentTimeMillis() + millis);
    }

    public boolean hasTimeout() {
        return (this.deadline != null);
    }

    @Nullable
    public Date getDeadline() {
        return this.deadline;
    }

    public int getTimeToLiveInSeconds() {
        double diff = ((double) getTimeToLiveInMillis()) / 1000;
        int secs = (int) Math.ceil(diff);
        checkTransactionTimeout(secs <= 0);
        return secs;
    }

    public long getTimeToLiveInMillis() throws TransactionTimedOutException {
        if (this.deadline == null) {
            throw new IllegalStateException("No timeout specified for this resource holder");
        }
        long timeToLive = this.deadline.getTime() - System.currentTimeMillis();
        checkTransactionTimeout(timeToLive <= 0);
        return timeToLive;
    }

    private void checkTransactionTimeout(boolean deadlineReached) throws TransactionTimedOutException {
        if (deadlineReached) {
            setRollbackOnly();
            throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
        }
    }

    public void requested() {
        this.referenceCount++;
    }

    public void released() {
        this.referenceCount--;
    }

    public boolean isOpen() {
        return (this.referenceCount > 0);
    }

    public void clear() {
        this.synchronizedWithTransaction = false;
        this.rollbackOnly = false;
        this.deadline = null;
    }

    @Override
    public void reset() {
        clear();
        this.referenceCount = 0;
    }

    @Override
    public void unbound() {
        this.isVoid = true;
    }

    @Override
    public boolean isVoid() {
        return this.isVoid;
    }

}
