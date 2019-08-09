package org.springframework.transaction.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.core.InfrastructureProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.List;

public abstract class TransactionSynchronizationUtils {

    private static final Log logger = LogFactory.getLog(TransactionSynchronizationUtils.class);

    private static final boolean aopAvailable = ClassUtils.isPresent("org.springframework.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());

    public static boolean sameResourceFactory(ResourceTransactionManager tm, Object resourceFactory) {
        return unwrapResourceIfNecessary(tm.getResourceFactory()).equals(unwrapResourceIfNecessary(resourceFactory));
    }

    static Object unwrapResourceIfNecessary(Object resource) {
        Assert.notNull(resource, "Resource must not be null");
        Object resourceRef = resource;
        // unwrap infrastructure proxy
        if (resourceRef instanceof InfrastructureProxy) {
            resourceRef = ((InfrastructureProxy) resourceRef).getWrappedObject();
        }
        if (aopAvailable) {
            // now unwrap scoped proxy
            resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
        }
        return resourceRef;
    }

    public static void triggerFlush() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.flush();
        }
    }

    public static void triggerBeforeCommit(boolean readOnly) {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.beforeCommit(readOnly);
        }
    }

    public static void triggerBeforeCompletion() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            try {
                synchronization.beforeCompletion();
            } catch (Throwable tsex) {
                logger.error("TransactionSynchronization.beforeCompletion threw exception", tsex);
            }
        }
    }

    public static void triggerAfterCommit() {
        invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());
    }

    public static void invokeAfterCommit(@Nullable List<TransactionSynchronization> synchronizations) {
        if (synchronizations != null) {
            for (TransactionSynchronization synchronization : synchronizations) {
                synchronization.afterCommit();
            }
        }
    }

    public static void triggerAfterCompletion(int completionStatus) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        invokeAfterCompletion(synchronizations, completionStatus);
    }

    public static void invokeAfterCompletion(@Nullable List<TransactionSynchronization> synchronizations, int completionStatus) {
        if (synchronizations != null) {
            for (TransactionSynchronization synchronization : synchronizations) {
                try {
                    synchronization.afterCompletion(completionStatus);
                } catch (Throwable tsex) {
                    logger.error("TransactionSynchronization.afterCompletion threw exception", tsex);
                }
            }
        }
    }

    private static class ScopedProxyUnwrapper {

        public static Object unwrapIfNecessary(Object resource) {
            if (resource instanceof ScopedObject) {
                return ((ScopedObject) resource).getTargetObject();
            } else {
                return resource;
            }
        }

    }

}
