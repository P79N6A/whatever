package org.springframework.transaction;

import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

public interface ReactiveTransactionManager extends TransactionManager {

    Mono<ReactiveTransaction> getReactiveTransaction(@Nullable TransactionDefinition definition) throws TransactionException;

    Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException;

    Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException;

}
