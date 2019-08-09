package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionException;

public interface TransactionOperations {

    @Nullable
    <T> T execute(TransactionCallback<T> action) throws TransactionException;

}
