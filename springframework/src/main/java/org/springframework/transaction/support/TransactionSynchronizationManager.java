package org.springframework.transaction.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

public abstract class TransactionSynchronizationManager {

    private static final Log logger = LogFactory.getLog(TransactionSynchronizationManager.class);

    private static final ThreadLocal<Map<Object, Object>> resources = new NamedThreadLocal<>("Transactional resources");

    private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations = new NamedThreadLocal<>("Transaction synchronizations");

    private static final ThreadLocal<String> currentTransactionName = new NamedThreadLocal<>("Current transaction name");

    private static final ThreadLocal<Boolean> currentTransactionReadOnly = new NamedThreadLocal<>("Current transaction read-only status");

    private static final ThreadLocal<Integer> currentTransactionIsolationLevel = new NamedThreadLocal<>("Current transaction isolation level");

    private static final ThreadLocal<Boolean> actualTransactionActive = new NamedThreadLocal<>("Actual transaction active");
    //-------------------------------------------------------------------------
    // Management of transaction-associated resource handles
    //-------------------------------------------------------------------------

    public static Map<Object, Object> getResourceMap() {
        Map<Object, Object> map = resources.get();
        return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
    }

    public static boolean hasResource(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doGetResource(actualKey);
        return (value != null);
    }

    @Nullable
    public static Object getResource(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doGetResource(actualKey);
        if (value != null && logger.isTraceEnabled()) {
            logger.trace("Retrieved value [" + value + "] for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
        }
        return value;
    }

    @Nullable
    private static Object doGetResource(Object actualKey) {
        Map<Object, Object> map = resources.get();
        if (map == null) {
            return null;
        }
        Object value = map.get(actualKey);
        // Transparently remove ResourceHolder that was marked as void...
        if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
            map.remove(actualKey);
            // Remove entire ThreadLocal if empty...
            if (map.isEmpty()) {
                resources.remove();
            }
            value = null;
        }
        return value;
    }

    public static void bindResource(Object key, Object value) throws IllegalStateException {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Assert.notNull(value, "Value must not be null");
        Map<Object, Object> map = resources.get();
        // set ThreadLocal Map if none found
        if (map == null) {
            map = new HashMap<>();
            resources.set(map);
        }
        Object oldValue = map.put(actualKey, value);
        // Transparently suppress a ResourceHolder that was marked as void...
        if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
            oldValue = null;
        }
        if (oldValue != null) {
            throw new IllegalStateException("Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Bound value [" + value + "] for key [" + actualKey + "] to thread [" + Thread.currentThread().getName() + "]");
        }
    }

    public static Object unbindResource(Object key) throws IllegalStateException {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doUnbindResource(actualKey);
        if (value == null) {
            throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
        }
        return value;
    }

    @Nullable
    public static Object unbindResourceIfPossible(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        return doUnbindResource(actualKey);
    }

    @Nullable
    private static Object doUnbindResource(Object actualKey) {
        Map<Object, Object> map = resources.get();
        if (map == null) {
            return null;
        }
        Object value = map.remove(actualKey);
        // Remove entire ThreadLocal if empty...
        if (map.isEmpty()) {
            resources.remove();
        }
        // Transparently suppress a ResourceHolder that was marked as void...
        if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
            value = null;
        }
        if (value != null && logger.isTraceEnabled()) {
            logger.trace("Removed value [" + value + "] for key [" + actualKey + "] from thread [" + Thread.currentThread().getName() + "]");
        }
        return value;
    }
    //-------------------------------------------------------------------------
    // Management of transaction synchronizations
    //-------------------------------------------------------------------------

    public static boolean isSynchronizationActive() {
        return (synchronizations.get() != null);
    }

    public static void initSynchronization() throws IllegalStateException {
        if (isSynchronizationActive()) {
            throw new IllegalStateException("Cannot activate transaction synchronization - already active");
        }
        logger.trace("Initializing transaction synchronization");
        synchronizations.set(new LinkedHashSet<>());
    }

    public static void registerSynchronization(TransactionSynchronization synchronization) throws IllegalStateException {
        Assert.notNull(synchronization, "TransactionSynchronization must not be null");
        Set<TransactionSynchronization> synchs = synchronizations.get();
        if (synchs == null) {
            throw new IllegalStateException("Transaction synchronization is not active");
        }
        synchs.add(synchronization);
    }

    public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
        Set<TransactionSynchronization> synchs = synchronizations.get();
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

    public static void clearSynchronization() throws IllegalStateException {
        if (!isSynchronizationActive()) {
            throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
        }
        logger.trace("Clearing transaction synchronization");
        synchronizations.remove();
    }
    //-------------------------------------------------------------------------
    // Exposure of transaction characteristics
    //-------------------------------------------------------------------------

    public static void setCurrentTransactionName(@Nullable String name) {
        currentTransactionName.set(name);
    }

    @Nullable
    public static String getCurrentTransactionName() {
        return currentTransactionName.get();
    }

    public static void setCurrentTransactionReadOnly(boolean readOnly) {
        currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
    }

    public static boolean isCurrentTransactionReadOnly() {
        return (currentTransactionReadOnly.get() != null);
    }

    public static void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
        currentTransactionIsolationLevel.set(isolationLevel);
    }

    @Nullable
    public static Integer getCurrentTransactionIsolationLevel() {
        return currentTransactionIsolationLevel.get();
    }

    public static void setActualTransactionActive(boolean active) {
        actualTransactionActive.set(active ? Boolean.TRUE : null);
    }

    public static boolean isActualTransactionActive() {
        return (actualTransactionActive.get() != null);
    }

    public static void clear() {
        synchronizations.remove();
        currentTransactionName.remove();
        currentTransactionReadOnly.remove();
        currentTransactionIsolationLevel.remove();
        actualTransactionActive.remove();
    }

}
