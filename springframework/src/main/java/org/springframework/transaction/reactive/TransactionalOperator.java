package org.springframework.transaction.reactive;

import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionalOperator {

    default <T> Flux<T> transactional(Flux<T> flux) {
        return execute(it -> flux);
    }

    default <T> Mono<T> transactional(Mono<T> mono) {
        return execute(it -> mono).next();
    }

    <T> Flux<T> execute(TransactionCallback<T> action) throws TransactionException;
    // Static builder methods

    static TransactionalOperator create(ReactiveTransactionManager transactionManager) {
        return create(transactionManager, TransactionDefinition.withDefaults());
    }

    static TransactionalOperator create(ReactiveTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
        return new TransactionalOperatorImpl(transactionManager, transactionDefinition);
    }

}
