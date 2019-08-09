package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionStatus;

@FunctionalInterface
public interface TransactionCallback<T> {

    @Nullable
    T doInTransaction(TransactionStatus status);

}
