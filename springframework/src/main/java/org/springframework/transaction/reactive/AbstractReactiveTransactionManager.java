package org.springframework.transaction.reactive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@SuppressWarnings("serial")
public abstract class AbstractReactiveTransactionManager implements ReactiveTransactionManager, Serializable {

    protected transient Log logger = LogFactory.getLog(getClass());
    //---------------------------------------------------------------------
    // Implementation of ReactiveTransactionManager
    //---------------------------------------------------------------------

    @Override
    public final Mono<ReactiveTransaction> getReactiveTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
        // Use defaults if no transaction definition given.
        TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
        return TransactionSynchronizationManager.currentTransaction().flatMap(synchronizationManager -> {
            Object transaction = doGetTransaction(synchronizationManager);
            // Cache debug flag to avoid repeated checks.
            boolean debugEnabled = logger.isDebugEnabled();
            if (isExistingTransaction(transaction)) {
                // Existing transaction found -> check propagation behavior to find out how to behave.
                return handleExistingTransaction(synchronizationManager, def, transaction, debugEnabled);
            }
            // Check definition settings for new transaction.
            if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
                return Mono.error(new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout()));
            }
            // No existing transaction found -> check propagation behavior to find out how to proceed.
            if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
                return Mono.error(new IllegalTransactionStateException("No existing transaction found for transaction marked with propagation 'mandatory'"));
            } else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED || def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW || def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
                return TransactionContextManager.currentContext().map(TransactionSynchronizationManager::new).flatMap(nestedSynchronizationManager -> suspend(nestedSynchronizationManager, null).map(Optional::of).defaultIfEmpty(Optional.empty()).flatMap(suspendedResources -> {
                    if (debugEnabled) {
                        logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
                    }
                    return Mono.defer(() -> {
                        GenericReactiveTransaction status = newReactiveTransaction(nestedSynchronizationManager, def, transaction, true, debugEnabled, suspendedResources.orElse(null));
                        return doBegin(nestedSynchronizationManager, transaction, def).doOnSuccess(ignore -> prepareSynchronization(nestedSynchronizationManager, status, def)).thenReturn(status);
                    }).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, ex -> resume(nestedSynchronizationManager, null, suspendedResources.orElse(null)).then(Mono.error(ex)));
                }));
            } else {
                // Create "empty" transaction: no actual transaction, but potentially synchronization.
                if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
                    logger.warn("Custom isolation level specified but no actual transaction initiated; " + "isolation level will effectively be ignored: " + def);
                }
                return Mono.just(prepareReactiveTransaction(synchronizationManager, def, null, true, debugEnabled, null));
            }
        });
    }

    private Mono<ReactiveTransaction> handleExistingTransaction(TransactionSynchronizationManager synchronizationManager, TransactionDefinition definition, Object transaction, boolean debugEnabled) throws TransactionException {
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
            return Mono.error(new IllegalTransactionStateException("Existing transaction found for transaction marked with propagation 'never'"));
        }
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
            if (debugEnabled) {
                logger.debug("Suspending current transaction");
            }
            Mono<SuspendedResourcesHolder> suspend = suspend(synchronizationManager, transaction);
            return suspend.map(suspendedResources -> prepareReactiveTransaction(synchronizationManager, definition, null, false, debugEnabled, suspendedResources)) //
                    .switchIfEmpty(Mono.fromSupplier(() -> prepareReactiveTransaction(synchronizationManager, definition, null, false, debugEnabled, null))).cast(ReactiveTransaction.class);
        }
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
            if (debugEnabled) {
                logger.debug("Suspending current transaction, creating new transaction with name [" + definition.getName() + "]");
            }
            Mono<SuspendedResourcesHolder> suspendedResources = suspend(synchronizationManager, transaction);
            return suspendedResources.flatMap(suspendedResourcesHolder -> {
                GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager, definition, transaction, true, debugEnabled, suspendedResourcesHolder);
                return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore -> prepareSynchronization(synchronizationManager, status, definition)).thenReturn(status).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, beginEx -> resumeAfterBeginException(synchronizationManager, transaction, suspendedResourcesHolder, beginEx).then(Mono.error(beginEx)));
            });
        }
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
            if (debugEnabled) {
                logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
            }
            // Nested transaction through nested begin and commit/rollback calls.
            GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager, definition, transaction, true, debugEnabled, null);
            return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore -> prepareSynchronization(synchronizationManager, status, definition)).thenReturn(status);
        }
        // Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
        if (debugEnabled) {
            logger.debug("Participating in existing transaction");
        }
        return Mono.just(prepareReactiveTransaction(synchronizationManager, definition, transaction, false, debugEnabled, null));
    }

    private GenericReactiveTransaction prepareReactiveTransaction(TransactionSynchronizationManager synchronizationManager, TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction, boolean debug, @Nullable Object suspendedResources) {
        GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager, definition, transaction, newTransaction, debug, suspendedResources);
        prepareSynchronization(synchronizationManager, status, definition);
        return status;
    }

    private GenericReactiveTransaction newReactiveTransaction(TransactionSynchronizationManager synchronizationManager, TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction, boolean debug, @Nullable Object suspendedResources) {
        return new GenericReactiveTransaction(transaction, newTransaction, !synchronizationManager.isSynchronizationActive(), definition.isReadOnly(), debug, suspendedResources);
    }

    private void prepareSynchronization(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status, TransactionDefinition definition) {
        if (status.isNewSynchronization()) {
            synchronizationManager.setActualTransactionActive(status.hasTransaction());
            synchronizationManager.setCurrentTransactionIsolationLevel(definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ? definition.getIsolationLevel() : null);
            synchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
            synchronizationManager.setCurrentTransactionName(definition.getName());
            synchronizationManager.initSynchronization();
        }
    }

    private Mono<SuspendedResourcesHolder> suspend(TransactionSynchronizationManager synchronizationManager, @Nullable Object transaction) throws TransactionException {
        if (synchronizationManager.isSynchronizationActive()) {
            Mono<List<TransactionSynchronization>> suspendedSynchronizations = doSuspendSynchronization(synchronizationManager);
            return suspendedSynchronizations.flatMap(synchronizations -> {
                Mono<Optional<Object>> suspendedResources = (transaction != null ? doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty()) : Mono.just(Optional.empty()));
                return suspendedResources.map(it -> {
                    String name = synchronizationManager.getCurrentTransactionName();
                    synchronizationManager.setCurrentTransactionName(null);
                    boolean readOnly = synchronizationManager.isCurrentTransactionReadOnly();
                    synchronizationManager.setCurrentTransactionReadOnly(false);
                    Integer isolationLevel = synchronizationManager.getCurrentTransactionIsolationLevel();
                    synchronizationManager.setCurrentTransactionIsolationLevel(null);
                    boolean wasActive = synchronizationManager.isActualTransactionActive();
                    synchronizationManager.setActualTransactionActive(false);
                    return new SuspendedResourcesHolder(it.orElse(null), synchronizations, name, readOnly, isolationLevel, wasActive);
                }).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, ex -> doResumeSynchronization(synchronizationManager, synchronizations).cast(SuspendedResourcesHolder.class));
            });
        } else if (transaction != null) {
            // Transaction active but no synchronization active.
            Mono<Optional<Object>> suspendedResources = doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty());
            return suspendedResources.map(it -> new SuspendedResourcesHolder(it.orElse(null)));
        } else {
            // Neither transaction nor synchronization active.
            return Mono.empty();
        }
    }

    private Mono<Void> resume(TransactionSynchronizationManager synchronizationManager, @Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder) throws TransactionException {
        Mono<Void> resume = Mono.empty();
        if (resourcesHolder != null) {
            Object suspendedResources = resourcesHolder.suspendedResources;
            if (suspendedResources != null) {
                resume = doResume(synchronizationManager, transaction, suspendedResources);
            }
            List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
            if (suspendedSynchronizations != null) {
                synchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
                synchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
                synchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
                synchronizationManager.setCurrentTransactionName(resourcesHolder.name);
                return resume.then(doResumeSynchronization(synchronizationManager, suspendedSynchronizations));
            }
        }
        return resume;
    }

    private Mono<Void> resumeAfterBeginException(TransactionSynchronizationManager synchronizationManager, Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {
        String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
        return resume(synchronizationManager, transaction, suspendedResources).doOnError(ErrorPredicates.RUNTIME_OR_ERROR, ex -> logger.error(exMessage, beginEx));
    }

    private Mono<List<TransactionSynchronization>> doSuspendSynchronization(TransactionSynchronizationManager synchronizationManager) {
        List<TransactionSynchronization> suspendedSynchronizations = synchronizationManager.getSynchronizations();
        return Flux.fromIterable(suspendedSynchronizations).concatMap(TransactionSynchronization::suspend).then(Mono.defer(() -> {
            synchronizationManager.clearSynchronization();
            return Mono.just(suspendedSynchronizations);
        }));
    }

    private Mono<Void> doResumeSynchronization(TransactionSynchronizationManager synchronizationManager, List<TransactionSynchronization> suspendedSynchronizations) {
        synchronizationManager.initSynchronization();
        return Flux.fromIterable(suspendedSynchronizations).concatMap(synchronization -> synchronization.resume().doOnSuccess(ignore -> synchronizationManager.registerSynchronization(synchronization))).then();
    }

    @Override
    public final Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException {
        if (transaction.isCompleted()) {
            return Mono.error(new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction"));
        }
        return TransactionSynchronizationManager.currentTransaction().flatMap(synchronizationManager -> {
            GenericReactiveTransaction reactiveTx = (GenericReactiveTransaction) transaction;
            if (reactiveTx.isRollbackOnly()) {
                if (reactiveTx.isDebug()) {
                    logger.debug("Transactional code has requested rollback");
                }
                return processRollback(synchronizationManager, reactiveTx);
            }
            return processCommit(synchronizationManager, reactiveTx);
        });
    }

    private Mono<Void> processCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
        AtomicBoolean beforeCompletionInvoked = new AtomicBoolean(false);
        Mono<Object> commit = prepareForCommit(synchronizationManager, status).then(triggerBeforeCommit(synchronizationManager, status)).then(triggerBeforeCompletion(synchronizationManager, status)).then(Mono.defer(() -> {
            beforeCompletionInvoked.set(true);
            if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Initiating transaction commit");
                }
                return doCommit(synchronizationManager, status);
            }
            return Mono.empty();
        })).then(Mono.empty().onErrorResume(ex -> {
            Mono<Object> propagateException = Mono.error(ex);
            // Store result in a local variable in order to appease the
            // Eclipse compiler with regard to inferred generics.
            Mono<Object> result = propagateException;
            if (ErrorPredicates.UNEXPECTED_ROLLBACK.test(ex)) {
                result = triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK).then(propagateException);
            } else if (ErrorPredicates.TRANSACTION_EXCEPTION.test(ex)) {
                result = triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN).then(propagateException);
            } else if (ErrorPredicates.RUNTIME_OR_ERROR.test(ex)) {
                Mono<Void> mono;
                if (!beforeCompletionInvoked.get()) {
                    mono = triggerBeforeCompletion(synchronizationManager, status);
                } else {
                    mono = Mono.empty();
                }
                result = mono.then(doRollbackOnCommitException(synchronizationManager, status, ex)).then(propagateException);
            }
            return result;
        })).then(Mono.defer(() -> triggerAfterCommit(synchronizationManager, status).onErrorResume(ex -> triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_COMMITTED).then(Mono.error(ex))).then(triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_COMMITTED))));
        return commit.onErrorResume(ex -> cleanupAfterCompletion(synchronizationManager, status).then(Mono.error(ex))).then(cleanupAfterCompletion(synchronizationManager, status));
    }

    @Override
    public final Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException {
        if (transaction.isCompleted()) {
            return Mono.error(new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction"));
        }
        return TransactionSynchronizationManager.currentTransaction().flatMap(synchronizationManager -> {
            GenericReactiveTransaction reactiveTx = (GenericReactiveTransaction) transaction;
            return processRollback(synchronizationManager, reactiveTx);
        });
    }

    private Mono<Void> processRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        return triggerBeforeCompletion(synchronizationManager, status).then(Mono.defer(() -> {
            if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Initiating transaction rollback");
                }
                return doRollback(synchronizationManager, status);
            } else {
                Mono<Void> beforeCompletion = Mono.empty();
                // Participating in larger transaction
                if (status.hasTransaction()) {
                    if (status.isDebug()) {
                        logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
                    }
                    beforeCompletion = doSetRollbackOnly(synchronizationManager, status);
                } else {
                    logger.debug("Should roll back transaction but cannot - no transaction available");
                }
                return beforeCompletion;
            }
        })).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, ex -> triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN).then(Mono.error(ex))).then(Mono.defer(() -> triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK))).onErrorResume(ex -> cleanupAfterCompletion(synchronizationManager, status).then(Mono.error(ex))).then(cleanupAfterCompletion(synchronizationManager, status));
    }

    private Mono<Void> doRollbackOnCommitException(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status, Throwable ex) throws TransactionException {
        return Mono.defer(() -> {
            if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Initiating transaction rollback after commit exception", ex);
                }
                return doRollback(synchronizationManager, status);
            } else if (status.hasTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
                }
                return doSetRollbackOnly(synchronizationManager, status);
            }
            return Mono.empty();
        }).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, rbex -> {
            logger.error("Commit exception overridden by rollback exception", ex);
            return triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN).then(Mono.error(rbex));
        }).then(triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private Mono<Void> triggerBeforeCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering beforeCommit synchronization");
            }
            return TransactionSynchronizationUtils.triggerBeforeCommit(synchronizationManager.getSynchronizations(), status.isReadOnly());
        }
        return Mono.empty();
    }

    private Mono<Void> triggerBeforeCompletion(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering beforeCompletion synchronization");
            }
            return TransactionSynchronizationUtils.triggerBeforeCompletion(synchronizationManager.getSynchronizations());
        }
        return Mono.empty();
    }

    private Mono<Void> triggerAfterCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering afterCommit synchronization");
            }
            return TransactionSynchronizationUtils.invokeAfterCommit(synchronizationManager.getSynchronizations());
        }
        return Mono.empty();
    }

    private Mono<Void> triggerAfterCompletion(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status, int completionStatus) {
        if (status.isNewSynchronization()) {
            List<TransactionSynchronization> synchronizations = synchronizationManager.getSynchronizations();
            synchronizationManager.clearSynchronization();
            if (!status.hasTransaction() || status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.trace("Triggering afterCompletion synchronization");
                }
                // No transaction or new transaction for the current scope ->
                // invoke the afterCompletion callbacks immediately
                return invokeAfterCompletion(synchronizationManager, synchronizations, completionStatus);
            } else if (!synchronizations.isEmpty()) {
                // Existing transaction that we participate in, controlled outside
                // of the scope of this Spring transaction manager -> try to register
                // an afterCompletion callback with the existing (JTA) transaction.
                return registerAfterCompletionWithExistingTransaction(synchronizationManager, status.getTransaction(), synchronizations);
            }
        }
        return Mono.empty();
    }

    private Mono<Void> invokeAfterCompletion(TransactionSynchronizationManager synchronizationManager, List<TransactionSynchronization> synchronizations, int completionStatus) {
        return TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
    }

    private Mono<Void> cleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        return Mono.defer(() -> {
            status.setCompleted();
            if (status.isNewSynchronization()) {
                synchronizationManager.clear();
            }
            Mono<Void> cleanup = Mono.empty();
            if (status.isNewTransaction()) {
                cleanup = doCleanupAfterCompletion(synchronizationManager, status.getTransaction());
            }
            if (status.getSuspendedResources() != null) {
                if (status.isDebug()) {
                    logger.debug("Resuming suspended transaction after completion of inner transaction");
                }
                Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
                return cleanup.then(resume(synchronizationManager, transaction, (SuspendedResourcesHolder) status.getSuspendedResources()));
            }
            return cleanup;
        });
    }
    //---------------------------------------------------------------------
    // Template methods to be implemented in subclasses
    //---------------------------------------------------------------------

    protected abstract Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) throws TransactionException;

    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        return false;
    }

    protected abstract Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition) throws TransactionException;

    protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager, Object transaction) throws TransactionException {
        throw new TransactionSuspensionNotSupportedException("Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
    }

    protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager, @Nullable Object transaction, Object suspendedResources) throws TransactionException {
        throw new TransactionSuspensionNotSupportedException("Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
    }

    protected Mono<Void> prepareForCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
        return Mono.empty();
    }

    protected abstract Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException;

    protected abstract Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException;

    protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
        throw new IllegalTransactionStateException("Participating in existing transactions is not supported - when 'isExistingTransaction' " + "returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
    }

    protected Mono<Void> registerAfterCompletionWithExistingTransaction(TransactionSynchronizationManager synchronizationManager, Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {
        logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " + "processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
        return invokeAfterCompletion(synchronizationManager, synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
    }

    protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager, Object transaction) {
        return Mono.empty();
    }
    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization; just initialize state after deserialization.
        ois.defaultReadObject();
        // Initialize transient fields.
        this.logger = LogFactory.getLog(getClass());
    }

    protected static final class SuspendedResourcesHolder {

        @Nullable
        private final Object suspendedResources;

        @Nullable
        private List<TransactionSynchronization> suspendedSynchronizations;

        @Nullable
        private String name;

        private boolean readOnly;

        @Nullable
        private Integer isolationLevel;

        private boolean wasActive;

        private SuspendedResourcesHolder(@Nullable Object suspendedResources) {
            this.suspendedResources = suspendedResources;
        }

        private SuspendedResourcesHolder(@Nullable Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations, @Nullable String name, boolean readOnly, @Nullable Integer isolationLevel, boolean wasActive) {
            this.suspendedResources = suspendedResources;
            this.suspendedSynchronizations = suspendedSynchronizations;
            this.name = name;
            this.readOnly = readOnly;
            this.isolationLevel = isolationLevel;
            this.wasActive = wasActive;
        }

    }

    private enum ErrorPredicates implements Predicate<Throwable> {

        RUNTIME_OR_ERROR {
            @Override
            public boolean test(Throwable throwable) {
                return throwable instanceof RuntimeException || throwable instanceof Error;
            }
        },

        TRANSACTION_EXCEPTION {
            @Override
            public boolean test(Throwable throwable) {
                return throwable instanceof TransactionException;
            }
        },

        UNEXPECTED_ROLLBACK {
            @Override
            public boolean test(Throwable throwable) {
                return throwable instanceof UnexpectedRollbackException;
            }
        };

        @Override
        public abstract boolean test(Throwable throwable);
    }

}
