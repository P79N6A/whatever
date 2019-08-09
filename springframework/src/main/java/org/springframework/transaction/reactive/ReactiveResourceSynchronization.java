package org.springframework.transaction.reactive;

import reactor.core.publisher.Mono;

public abstract class ReactiveResourceSynchronization<O, K> implements TransactionSynchronization {

    private final O resourceObject;

    private final K resourceKey;

    private final TransactionSynchronizationManager synchronizationManager;

    private volatile boolean holderActive = true;

    public ReactiveResourceSynchronization(O resourceObject, K resourceKey, TransactionSynchronizationManager synchronizationManager) {
        this.resourceObject = resourceObject;
        this.resourceKey = resourceKey;
        this.synchronizationManager = synchronizationManager;
    }

    @Override
    public Mono<Void> suspend() {
        if (this.holderActive) {
            this.synchronizationManager.unbindResource(this.resourceKey);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> resume() {
        if (this.holderActive) {
            this.synchronizationManager.bindResource(this.resourceKey, this.resourceObject);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> beforeCommit(boolean readOnly) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> beforeCompletion() {
        if (shouldUnbindAtCompletion()) {
            this.synchronizationManager.unbindResource(this.resourceKey);
            this.holderActive = false;
            if (shouldReleaseBeforeCompletion()) {
                return releaseResource(this.resourceObject, this.resourceKey);
            }
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> afterCommit() {
        if (!shouldReleaseBeforeCompletion()) {
            return processResourceAfterCommit(this.resourceObject);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> afterCompletion(int status) {
        return Mono.defer(() -> {
            Mono<Void> sync = Mono.empty();
            if (shouldUnbindAtCompletion()) {
                boolean releaseNecessary = false;
                if (this.holderActive) {
                    // The thread-bound resource holder might not be available anymore,
                    // since afterCompletion might get called from a different thread.
                    this.holderActive = false;
                    this.synchronizationManager.unbindResourceIfPossible(this.resourceKey);
                    releaseNecessary = true;
                } else {
                    releaseNecessary = shouldReleaseAfterCompletion(this.resourceObject);
                }
                if (releaseNecessary) {
                    sync = releaseResource(this.resourceObject, this.resourceKey);
                }
            } else {
                // Probably a pre-bound resource...
                sync = cleanupResource(this.resourceObject, this.resourceKey, (status == STATUS_COMMITTED));
            }
            return sync;
        });
    }

    protected boolean shouldUnbindAtCompletion() {
        return true;
    }

    protected boolean shouldReleaseBeforeCompletion() {
        return true;
    }

    protected boolean shouldReleaseAfterCompletion(O resourceHolder) {
        return !shouldReleaseBeforeCompletion();
    }

    protected Mono<Void> processResourceAfterCommit(O resourceHolder) {
        return Mono.empty();
    }

    protected Mono<Void> releaseResource(O resourceHolder, K resourceKey) {
        return Mono.empty();
    }

    protected Mono<Void> cleanupResource(O resourceHolder, K resourceKey, boolean committed) {
        return Mono.empty();
    }

}
