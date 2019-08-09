package org.springframework.transaction.reactive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.core.InfrastructureProxy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

abstract class TransactionSynchronizationUtils {

    private static final Log logger = LogFactory.getLog(TransactionSynchronizationUtils.class);

    private static final boolean aopAvailable = ClassUtils.isPresent("org.springframework.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());

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

    public static Mono<Void> triggerBeforeCommit(Collection<TransactionSynchronization> synchronizations, boolean readOnly) {
        return Flux.fromIterable(synchronizations).concatMap(it -> it.beforeCommit(readOnly)).then();
    }

    public static Mono<Void> triggerBeforeCompletion(Collection<TransactionSynchronization> synchronizations) {
        return Flux.fromIterable(synchronizations).concatMap(TransactionSynchronization::beforeCompletion).onErrorContinue((t, o) -> logger.error("TransactionSynchronization.beforeCompletion threw exception", t)).then();
    }

    public static Mono<Void> invokeAfterCommit(Collection<TransactionSynchronization> synchronizations) {
        return Flux.fromIterable(synchronizations).concatMap(TransactionSynchronization::afterCommit).then();
    }

    public static Mono<Void> invokeAfterCompletion(Collection<TransactionSynchronization> synchronizations, int completionStatus) {
        return Flux.fromIterable(synchronizations).concatMap(it -> it.afterCompletion(completionStatus)).onErrorContinue((t, o) -> logger.error("TransactionSynchronization.afterCompletion threw exception", t)).then();
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
