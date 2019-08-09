package org.springframework.transaction.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public abstract class AbstractPlatformTransactionManager implements PlatformTransactionManager, Serializable {

    public static final int SYNCHRONIZATION_ALWAYS = 0;

    public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;

    public static final int SYNCHRONIZATION_NEVER = 2;

    private static final Constants constants = new Constants(AbstractPlatformTransactionManager.class);

    protected transient Log logger = LogFactory.getLog(getClass());

    private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;

    private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;

    private boolean nestedTransactionAllowed = false;

    private boolean validateExistingTransaction = false;

    private boolean globalRollbackOnParticipationFailure = true;

    private boolean failEarlyOnGlobalRollbackOnly = false;

    private boolean rollbackOnCommitFailure = false;

    public final void setTransactionSynchronizationName(String constantName) {
        setTransactionSynchronization(constants.asNumber(constantName).intValue());
    }

    public final void setTransactionSynchronization(int transactionSynchronization) {
        this.transactionSynchronization = transactionSynchronization;
    }

    public final int getTransactionSynchronization() {
        return this.transactionSynchronization;
    }

    public final void setDefaultTimeout(int defaultTimeout) {
        if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
            throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
        }
        this.defaultTimeout = defaultTimeout;
    }

    public final int getDefaultTimeout() {
        return this.defaultTimeout;
    }

    public final void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
        this.nestedTransactionAllowed = nestedTransactionAllowed;
    }

    public final boolean isNestedTransactionAllowed() {
        return this.nestedTransactionAllowed;
    }

    public final void setValidateExistingTransaction(boolean validateExistingTransaction) {
        this.validateExistingTransaction = validateExistingTransaction;
    }

    public final boolean isValidateExistingTransaction() {
        return this.validateExistingTransaction;
    }

    public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
        this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
    }

    public final boolean isGlobalRollbackOnParticipationFailure() {
        return this.globalRollbackOnParticipationFailure;
    }

    public final void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
        this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
    }

    public final boolean isFailEarlyOnGlobalRollbackOnly() {
        return this.failEarlyOnGlobalRollbackOnly;
    }

    public final void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
        this.rollbackOnCommitFailure = rollbackOnCommitFailure;
    }

    public final boolean isRollbackOnCommitFailure() {
        return this.rollbackOnCommitFailure;
    }
    //---------------------------------------------------------------------
    // Implementation of PlatformTransactionManager
    //---------------------------------------------------------------------

    /**
     * 获取事务
     */
    @Override
    public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
        // Use defaults if no transaction definition given.
        TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
        // 模板方法，获取当前线程事务，主要是从ThreadLocal中获取当前线程的Connection
        Object transaction = doGetTransaction();
        boolean debugEnabled = logger.isDebugEnabled();
        // 如果当前线程已存在事务，则按照事务的传播机制来判断是否使用当前事务
        if (isExistingTransaction(transaction)) {
            // Existing transaction found -> check propagation behavior to find out how to behave.
            return handleExistingTransaction(def, transaction, debugEnabled);
        }
        // 查看事务是否已过时
        if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
            throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
        }
        // 说明当前线程不存在事务，不符合MANDATORY传播机制
        if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
            throw new IllegalTransactionStateException("No existing transaction found for transaction marked with propagation 'mandatory'");
        }
        // 对于REQUIRED、REQUIRES_NEW、NESTED这三种传播机制
        else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED || def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW || def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
            SuspendedResourcesHolder suspendedResources = suspend(null);
            if (debugEnabled) {
                logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
            }
            // 当前线程之前不存在事务，创建新的事务
            try {
                boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
                DefaultTransactionStatus status = newTransactionStatus(def, transaction, true, newSynchronization, debugEnabled, suspendedResources);
                // 模板方法，创建新的Connection，这里为DataSourceTransactionManager
                doBegin(transaction, def);
                prepareSynchronization(status, def);
                return status;
            } catch (RuntimeException | Error ex) {
                resume(null, suspendedResources);
                throw ex;
            }
        } else {
            // Create "empty" transaction: no actual transaction, but potentially synchronization.
            if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
                logger.warn("Custom isolation level specified but no actual transaction initiated; " + "isolation level will effectively be ignored: " + def);
            }
            boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
            return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
        }
    }

    private TransactionStatus handleExistingTransaction(TransactionDefinition definition, Object transaction, boolean debugEnabled) throws TransactionException {
        // 隔离级别是NEVER，说明不需要当前线程的已存在事务
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
            throw new IllegalTransactionStateException("Existing transaction found for transaction marked with propagation 'never'");
        }
        // NOT_SUPPORTED，不需要Spring创建事务
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
            if (debugEnabled) {
                logger.debug("Suspending current transaction");
            }
            Object suspendedResources = suspend(transaction);
            boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
            return prepareTransactionStatus(definition, null, false, newSynchronization, debugEnabled, suspendedResources);
        }
        // REQUIRES_NEW，挂起当前事务，直接创建一个新的事务
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
            if (debugEnabled) {
                logger.debug("Suspending current transaction, creating new transaction with name [" + definition.getName() + "]");
            }
            SuspendedResourcesHolder suspendedResources = suspend(transaction);
            try {
                boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
                DefaultTransactionStatus status = newTransactionStatus(definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
                doBegin(transaction, definition);
                prepareSynchronization(status, definition);
                return status;
            } catch (RuntimeException | Error beginEx) {
                resumeAfterBeginException(transaction, suspendedResources, beginEx);
                throw beginEx;
            }
        }
        // NESTED，创建当前事务的一个嵌套事务，嵌套事务独立于之前的线程事务进行提交或回滚
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
            if (!isNestedTransactionAllowed()) {
                throw new NestedTransactionNotSupportedException("Transaction manager does not allow nested transactions by default - " + "specify 'nestedTransactionAllowed' property with value 'true'");
            }
            if (debugEnabled) {
                logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
            }
            if (useSavepointForNestedTransaction()) {
                // Create savepoint within existing Spring-managed transaction,
                // through the SavepointManager API implemented by TransactionStatus.
                // Usually uses JDBC 3.0 savepoints. Never activates Spring synchronization.
                DefaultTransactionStatus status = prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);
                status.createAndHoldSavepoint();
                return status;
            } else {
                // Nested transaction through nested begin and commit/rollback calls.
                // Usually only for JTA: Spring synchronization might get activated here
                // in case of a pre-existing JTA transaction.
                boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
                DefaultTransactionStatus status = newTransactionStatus(definition, transaction, true, newSynchronization, debugEnabled, null);
                doBegin(transaction, definition);
                prepareSynchronization(status, definition);
                return status;
            }
        }
        // Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
        if (debugEnabled) {
            logger.debug("Participating in existing transaction");
        }
        if (isValidateExistingTransaction()) {
            if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
                Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
                if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
                    Constants isoConstants = DefaultTransactionDefinition.constants;
                    throw new IllegalTransactionStateException("Participating transaction with definition [" + definition + "] specifies isolation level which is incompatible with existing transaction: " + (currentIsolationLevel != null ? isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) : "(unknown)"));
                }
            }
            if (!definition.isReadOnly()) {
                if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                    throw new IllegalTransactionStateException("Participating transaction with definition [" + definition + "] is not marked as read-only but existing transaction is");
                }
            }
        }
        boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
        return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
    }

    protected final DefaultTransactionStatus prepareTransactionStatus(TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {
        DefaultTransactionStatus status = newTransactionStatus(definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
        prepareSynchronization(status, definition);
        return status;
    }

    protected DefaultTransactionStatus newTransactionStatus(TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {
        boolean actualNewSynchronization = newSynchronization && !TransactionSynchronizationManager.isSynchronizationActive();
        return new DefaultTransactionStatus(transaction, newTransaction, actualNewSynchronization, definition.isReadOnly(), debug, suspendedResources);
    }

    protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
        if (status.isNewSynchronization()) {
            TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ? definition.getIsolationLevel() : null);
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
            TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    protected int determineTimeout(TransactionDefinition definition) {
        if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
            return definition.getTimeout();
        }
        return getDefaultTimeout();
    }

    @Nullable
    protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
            try {
                Object suspendedResources = null;
                if (transaction != null) {
                    suspendedResources = doSuspend(transaction);
                }
                String name = TransactionSynchronizationManager.getCurrentTransactionName();
                TransactionSynchronizationManager.setCurrentTransactionName(null);
                boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
                Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
                TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
                boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
                TransactionSynchronizationManager.setActualTransactionActive(false);
                return new SuspendedResourcesHolder(suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
            } catch (RuntimeException | Error ex) {
                // doSuspend failed - original transaction is still active...
                doResumeSynchronization(suspendedSynchronizations);
                throw ex;
            }
        } else if (transaction != null) {
            // Transaction active but no synchronization active.
            Object suspendedResources = doSuspend(transaction);
            return new SuspendedResourcesHolder(suspendedResources);
        } else {
            // Neither transaction nor synchronization active.
            return null;
        }
    }

    protected final void resume(@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder) throws TransactionException {
        if (resourcesHolder != null) {
            Object suspendedResources = resourcesHolder.suspendedResources;
            if (suspendedResources != null) {
                doResume(transaction, suspendedResources);
            }
            List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
            if (suspendedSynchronizations != null) {
                TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
                TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
                TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
                doResumeSynchronization(suspendedSynchronizations);
            }
        }
    }

    private void resumeAfterBeginException(Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {
        String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
        try {
            resume(transaction, suspendedResources);
        } catch (RuntimeException | Error resumeEx) {
            logger.error(exMessage, beginEx);
            throw resumeEx;
        }
    }

    private List<TransactionSynchronization> doSuspendSynchronization() {
        List<TransactionSynchronization> suspendedSynchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : suspendedSynchronizations) {
            synchronization.suspend();
        }
        TransactionSynchronizationManager.clearSynchronization();
        return suspendedSynchronizations;
    }

    private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
        TransactionSynchronizationManager.initSynchronization();
        for (TransactionSynchronization synchronization : suspendedSynchronizations) {
            synchronization.resume();
            TransactionSynchronizationManager.registerSynchronization(synchronization);
        }
    }

    @Override
    public final void commit(TransactionStatus status) throws TransactionException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
        }
        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        // 如果已经被标记回滚，不提交事务，直接回滚
        if (defStatus.isLocalRollbackOnly()) {
            if (defStatus.isDebug()) {
                logger.debug("Transactional code has requested rollback");
            }
            processRollback(defStatus, false);
            return;
        }
        if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
            if (defStatus.isDebug()) {
                logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
            }
            processRollback(defStatus, true);
            return;
        }
        // 事务提交
        processCommit(defStatus);
    }

    private void processCommit(DefaultTransactionStatus status) throws TransactionException {
        try {
            boolean beforeCompletionInvoked = false;
            try {
                boolean unexpectedRollback = false;
                prepareForCommit(status);
                triggerBeforeCommit(status);
                triggerBeforeCompletion(status);
                beforeCompletionInvoked = true;
                if (status.hasSavepoint()) {
                    if (status.isDebug()) {
                        logger.debug("Releasing transaction savepoint");
                    }
                    unexpectedRollback = status.isGlobalRollbackOnly();
                    status.releaseHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    if (status.isDebug()) {
                        logger.debug("Initiating transaction commit");
                    }
                    unexpectedRollback = status.isGlobalRollbackOnly();
                    doCommit(status);
                } else if (isFailEarlyOnGlobalRollbackOnly()) {
                    unexpectedRollback = status.isGlobalRollbackOnly();
                }
                // Throw UnexpectedRollbackException if we have a global rollback-only
                // marker but still didn't get a corresponding exception from commit.
                if (unexpectedRollback) {
                    throw new UnexpectedRollbackException("Transaction silently rolled back because it has been marked as rollback-only");
                }
            } catch (UnexpectedRollbackException ex) {
                // can only be caused by doCommit
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
                throw ex;
            } catch (TransactionException ex) {
                // can only be caused by doCommit
                if (isRollbackOnCommitFailure()) {
                    doRollbackOnCommitException(status, ex);
                } else {
                    triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
                }
                throw ex;
            } catch (RuntimeException | Error ex) {
                if (!beforeCompletionInvoked) {
                    triggerBeforeCompletion(status);
                }
                doRollbackOnCommitException(status, ex);
                throw ex;
            }
            // Trigger afterCommit callbacks, with an exception thrown there
            // propagated to callers but the transaction still considered as committed.
            try {
                triggerAfterCommit(status);
            } finally {
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
            }

        } finally {
            cleanupAfterCompletion(status);
        }
    }

    @Override
    public final void rollback(TransactionStatus status) throws TransactionException {
        // 如果是已完成状态，直接抛异常
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
        }
        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        // 回滚
        processRollback(defStatus, false);
    }

    private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
        try {
            boolean unexpectedRollback = unexpected;
            try {
                triggerBeforeCompletion(status);
                if (status.hasSavepoint()) {
                    if (status.isDebug()) {
                        logger.debug("Rolling back transaction to savepoint");
                    }
                    status.rollbackToHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    if (status.isDebug()) {
                        logger.debug("Initiating transaction rollback");
                    }
                    doRollback(status);
                } else {
                    // Participating in larger transaction
                    if (status.hasTransaction()) {
                        if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
                            if (status.isDebug()) {
                                logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
                            }
                            doSetRollbackOnly(status);
                        } else {
                            if (status.isDebug()) {
                                logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
                            }
                        }
                    } else {
                        logger.debug("Should roll back transaction but cannot - no transaction available");
                    }
                    // Unexpected rollback only matters here if we're asked to fail early
                    if (!isFailEarlyOnGlobalRollbackOnly()) {
                        unexpectedRollback = false;
                    }
                }
            } catch (RuntimeException | Error ex) {
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
                throw ex;
            }
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
            // Raise UnexpectedRollbackException if we had a global rollback-only marker
            if (unexpectedRollback) {
                throw new UnexpectedRollbackException("Transaction rolled back because it has been marked as rollback-only");
            }
        } finally {
            cleanupAfterCompletion(status);
        }
    }

    private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
        try {
            if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Initiating transaction rollback after commit exception", ex);
                }
                doRollback(status);
            } else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
                if (status.isDebug()) {
                    logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
                }
                doSetRollbackOnly(status);
            }
        } catch (RuntimeException | Error rbex) {
            logger.error("Commit exception overridden by rollback exception", ex);
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
            throw rbex;
        }
        triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
    }

    protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering beforeCommit synchronization");
            }
            TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
        }
    }

    protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering beforeCompletion synchronization");
            }
            TransactionSynchronizationUtils.triggerBeforeCompletion();
        }
    }

    private void triggerAfterCommit(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering afterCommit synchronization");
            }
            TransactionSynchronizationUtils.triggerAfterCommit();
        }
    }

    private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus) {
        if (status.isNewSynchronization()) {
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();
            if (!status.hasTransaction() || status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.trace("Triggering afterCompletion synchronization");
                }
                // No transaction or new transaction for the current scope ->
                // invoke the afterCompletion callbacks immediately
                invokeAfterCompletion(synchronizations, completionStatus);
            } else if (!synchronizations.isEmpty()) {
                // Existing transaction that we participate in, controlled outside
                // of the scope of this Spring transaction manager -> try to register
                // an afterCompletion callback with the existing (JTA) transaction.
                registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
            }
        }
    }

    protected final void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
        TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
    }

    private void cleanupAfterCompletion(DefaultTransactionStatus status) {
        status.setCompleted();
        if (status.isNewSynchronization()) {
            TransactionSynchronizationManager.clear();
        }
        if (status.isNewTransaction()) {
            doCleanupAfterCompletion(status.getTransaction());
        }
        if (status.getSuspendedResources() != null) {
            if (status.isDebug()) {
                logger.debug("Resuming suspended transaction after completion of inner transaction");
            }
            Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
            resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
        }
    }
    //---------------------------------------------------------------------
    // Template methods to be implemented in subclasses
    //---------------------------------------------------------------------

    protected abstract Object doGetTransaction() throws TransactionException;

    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        return false;
    }

    protected boolean useSavepointForNestedTransaction() {
        return true;
    }

    protected abstract void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException;

    protected Object doSuspend(Object transaction) throws TransactionException {
        throw new TransactionSuspensionNotSupportedException("Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
    }

    protected void doResume(@Nullable Object transaction, Object suspendedResources) throws TransactionException {
        throw new TransactionSuspensionNotSupportedException("Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
    }

    protected boolean shouldCommitOnGlobalRollbackOnly() {
        return false;
    }

    protected void prepareForCommit(DefaultTransactionStatus status) {
    }

    protected abstract void doCommit(DefaultTransactionStatus status) throws TransactionException;

    protected abstract void doRollback(DefaultTransactionStatus status) throws TransactionException;

    protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
        throw new IllegalTransactionStateException("Participating in existing transactions is not supported - when 'isExistingTransaction' " + "returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
    }

    protected void registerAfterCompletionWithExistingTransaction(Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {
        logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " + "processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
        invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
    }

    protected void doCleanupAfterCompletion(Object transaction) {
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

        private SuspendedResourcesHolder(Object suspendedResources) {
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

}
