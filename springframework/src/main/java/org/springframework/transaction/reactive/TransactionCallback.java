package org.springframework.transaction.reactive;

import org.reactivestreams.Publisher;
import org.springframework.transaction.ReactiveTransaction;

@FunctionalInterface
public interface TransactionCallback<T> {

    Publisher<T> doInTransaction(ReactiveTransaction status);

}
