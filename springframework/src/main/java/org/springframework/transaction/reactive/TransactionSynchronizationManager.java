package org.springframework.transaction.reactive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.*;

public class TransactionSynchronizationManager {

    private static final Log logger = LogFactory.getLog(TransactionSynchronizationManager.class);

    private final TransactionContext transactionContext;

    public TransactionSynchronizationManager(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    public static Mono<TransactionSynchronizationManager> currentTransaction() {
        return TransactionContextManager.currentContext().map(TransactionSynchronizationManager::new);
    }

    public boolean hasResource(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doGetResource(actualKey);
        return (value != null);
    }

    @Nullable
    public Object getResource(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doGetResource(actualKey);
        if (value != null && logger.isTraceEnabled()) {
            logger.trace("Retrieved value [" + value + "] for key [" + actualKey + "] bound to context [" + this.transactionContext.getName() + "]");
        }
        return value;
    }

    @Nullable
    private Object doGetResource(Object actualKey) {
        Map<Object, Object> map = this.transactionContext.getResources();
        Object value = map.get(actualKey);
        return value;
    }

    public void bindResource(Object key, Object value) throws IllegalStateException {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Assert.notNull(value, "Value must not be null");
        Map<Object, Object> map = this.transactionContext.getResources();
        Object oldValue = map.put(actualKey, value);
        if (oldValue != null) {
            throw new IllegalStateException("Already value [" + oldValue + "] for key [" + actualKey + "] bound to context [" + this.transactionContext.getName() + "]");
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Bound value [" + value + "] for key [" + actualKey + "] to context [" + this.transactionContext.getName() + "]");
        }
    }

    public Object unbindResource(Object key) throws IllegalStateException {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doUnbindResource(actualKey);
        if (value == null) {
            throw new IllegalStateException("No value for key [" + actualKey + "] bound to context [" + this.transactionContext.getName() + "]");
        }
        return value;
    }

    @Nullable
    public Object unbindResourceIfPossible(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        return doUnbindResource(actualKey);
    }

    @Nullable
    private Object doUnbindResource(Object actualKey) {
        Map<Object, Object> map = this.transactionContext.getResources();
        Object value = map.remove(actualKey);
        if (value != null && logger.isTraceEnabled()) {
            logger.trace("Removed value [" + value + "] for key [" + actualKey + "] from context [" + this.transactionContext.getName() + "]");
        }
        return value;
    }
    //-------------------------------------------------------------------------
    // Management of transaction synchronizations
    //-------------------------------------------------------------------------

    public boolean isSynchronizationActive() {
        return (this.transactionContext.getSynchronizations() != null);
    }

    public void initSynchronization() throws IllegalStateException {
        if (isSynchronizationActive()) {
            throw new IllegalStateException("Cannot activate transaction synchronization - already active");
        }
        logger.trace("Initializing transaction synchronization");
        this.transactionContext.setSynchronizations(new LinkedHashSet<>());
    }

    public void registerSynchronization(TransactionSynchronization synchronization) throws IllegalStateException {
        Assert.notNull(synchronization, "TransactionSynchronization must not be null");
        Set<TransactionSynchronization> synchs = this.transactionContext.getSynchronizations();
        if (synchs == null) {
            throw new IllegalStateException("Transaction synchronization is not active");
        }
        synchs.add(synchronization);
    }

    public List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
        Set<TransactionSynchronization> synchs = this.transactionContext.getSynchronizations();
        if (synchs == null) {
            throw new IllegalStateException("Transaction synchronization is not active");
        }
        // Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
        // while iterating and invoking synchronization callbacks that in turn
        // might register further synchronizations.
        if (synchs.isEmpty()) {
            return Collections.emptyList();
        } else {
            // Sort lazily here, not in registerSynchronization.
            List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
            AnnotationAwareOrderComparator.sort(sortedSynchs);
            return Collections.unmodifiableList(sortedSynchs);
        }
    }

    public void clearSynchronization() throws IllegalStateException {
        if (!isSynchronizationActive()) {
            throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
        }
        logger.trace("Clearing transaction synchronization");
        this.transactionContext.setSynchronizations(null);
    }
    //-------------------------------------------------------------------------
    // Exposure of transaction characteristics
    //-------------------------------------------------------------------------

    public void setCurrentTransactionName(@Nullable String name) {
        this.transactionContext.setCurrentTransactionName(name);
    }

    @Nullable
    public String getCurrentTransactionName() {
        return this.transactionContext.getCurrentTransactionName();
    }

    public void setCurrentTransactionReadOnly(boolean readOnly) {
        this.transactionContext.setCurrentTransactionReadOnly(readOnly);
    }

    public boolean isCurrentTransactionReadOnly() {
        return this.transactionContext.isCurrentTransactionReadOnly();
    }

    public void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
        this.transactionContext.setCurrentTransactionIsolationLevel(isolationLevel);
    }

    @Nullable
    public Integer getCurrentTransactionIsolationLevel() {
        return this.transactionContext.getCurrentTransactionIsolationLevel();
    }

    public void setActualTransactionActive(boolean active) {
        this.transactionContext.setActualTransactionActive(active);
    }

    public boolean isActualTransactionActive() {
        return this.transactionContext.isActualTransactionActive();
    }

    public void clear() {
        this.transactionContext.clear();
    }

}
