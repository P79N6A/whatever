package org.springframework.transaction.reactive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.*;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("serial")
final class TransactionalOperatorImpl implements TransactionalOperator {

    private static final Log logger = LogFactory.getLog(TransactionalOperatorImpl.class);

    private final ReactiveTransactionManager transactionManager;

    private final TransactionDefinition transactionDefinition;

    TransactionalOperatorImpl(ReactiveTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
        Assert.notNull(transactionManager, "ReactiveTransactionManager must not be null");
        Assert.notNull(transactionManager, "TransactionDefinition must not be null");
        this.transactionManager = transactionManager;
        this.transactionDefinition = transactionDefinition;
    }

    public ReactiveTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    @Override
    public <T> Flux<T> execute(TransactionCallback<T> action) throws TransactionException {
        return TransactionContextManager.currentContext().flatMapMany(context -> {
            Mono<ReactiveTransaction> status = this.transactionManager.getReactiveTransaction(this.transactionDefinition);
            // This is an around advice: Invoke the next interceptor in the chain.
            // This will normally result in a target object being invoked.
            // Need re-wrapping of ReactiveTransaction until we get hold of the exception
            // through usingWhen.
            return status.flatMapMany(it -> Flux.usingWhen(Mono.just(it), action::doInTransaction, this.transactionManager::commit, s -> Mono.empty()).onErrorResume(ex -> rollbackOnException(it, ex).then(Mono.error(ex))));
        }).subscriberContext(TransactionContextManager.getOrCreateContext()).subscriberContext(TransactionContextManager.getOrCreateContextHolder());
    }

    private Mono<Void> rollbackOnException(ReactiveTransaction status, Throwable ex) throws TransactionException {
        logger.debug("Initiating transaction rollback on application exception", ex);
        return this.transactionManager.rollback(status).onErrorMap(ex2 -> {
            logger.error("Application exception overridden by rollback exception", ex);
            if (ex2 instanceof TransactionSystemException) {
                ((TransactionSystemException) ex2).initApplicationException(ex);
            }
            return ex2;
        });
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (super.equals(other) && (!(other instanceof TransactionalOperatorImpl) || getTransactionManager() == ((TransactionalOperatorImpl) other).getTransactionManager())));
    }

    @Override
    public int hashCode() {
        return getTransactionManager().hashCode();
    }

}
