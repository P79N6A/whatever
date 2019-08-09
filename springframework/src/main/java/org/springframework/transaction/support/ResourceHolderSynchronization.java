package org.springframework.transaction.support;

public abstract class ResourceHolderSynchronization<H extends ResourceHolder, K> implements TransactionSynchronization {

    private final H resourceHolder;

    private final K resourceKey;

    private volatile boolean holderActive = true;

    public ResourceHolderSynchronization(H resourceHolder, K resourceKey) {
        this.resourceHolder = resourceHolder;
        this.resourceKey = resourceKey;
    }

    @Override
    public void suspend() {
        if (this.holderActive) {
            TransactionSynchronizationManager.unbindResource(this.resourceKey);
        }
    }

    @Override
    public void resume() {
        if (this.holderActive) {
            TransactionSynchronizationManager.bindResource(this.resourceKey, this.resourceHolder);
        }
    }

    @Override
    public void flush() {
        flushResource(this.resourceHolder);
    }

    @Override
    public void beforeCommit(boolean readOnly) {
    }

    @Override
    public void beforeCompletion() {
        if (shouldUnbindAtCompletion()) {
            TransactionSynchronizationManager.unbindResource(this.resourceKey);
            this.holderActive = false;
            if (shouldReleaseBeforeCompletion()) {
                releaseResource(this.resourceHolder, this.resourceKey);
            }
        }
    }

    @Override
    public void afterCommit() {
        if (!shouldReleaseBeforeCompletion()) {
            processResourceAfterCommit(this.resourceHolder);
        }
    }

    @Override
    public void afterCompletion(int status) {
        if (shouldUnbindAtCompletion()) {
            boolean releaseNecessary = false;
            if (this.holderActive) {
                // The thread-bound resource holder might not be available anymore,
                // since afterCompletion might get called from a different thread.
                this.holderActive = false;
                TransactionSynchronizationManager.unbindResourceIfPossible(this.resourceKey);
                this.resourceHolder.unbound();
                releaseNecessary = true;
            } else {
                releaseNecessary = shouldReleaseAfterCompletion(this.resourceHolder);
            }
            if (releaseNecessary) {
                releaseResource(this.resourceHolder, this.resourceKey);
            }
        } else {
            // Probably a pre-bound resource...
            cleanupResource(this.resourceHolder, this.resourceKey, (status == STATUS_COMMITTED));
        }
        this.resourceHolder.reset();
    }

    protected boolean shouldUnbindAtCompletion() {
        return true;
    }

    protected boolean shouldReleaseBeforeCompletion() {
        return true;
    }

    protected boolean shouldReleaseAfterCompletion(H resourceHolder) {
        return !shouldReleaseBeforeCompletion();
    }

    protected void flushResource(H resourceHolder) {
    }

    protected void processResourceAfterCommit(H resourceHolder) {
    }

    protected void releaseResource(H resourceHolder, K resourceKey) {
    }

    protected void cleanupResource(H resourceHolder, K resourceKey, boolean committed) {
    }

}
