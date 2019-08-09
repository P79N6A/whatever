package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionStatus;

public abstract class TransactionCallbackWithoutResult implements TransactionCallback<Object> {

    @Override
    @Nullable
    public final Object doInTransaction(TransactionStatus status) {
        doInTransactionWithoutResult(status);
        return null;
    }

    protected abstract void doInTransactionWithoutResult(TransactionStatus status);

}
