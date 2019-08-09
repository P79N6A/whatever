package org.springframework.transaction.jta;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public class SimpleTransactionFactory implements TransactionFactory {

    private final TransactionManager transactionManager;

    public SimpleTransactionFactory(TransactionManager transactionManager) {
        Assert.notNull(transactionManager, "TransactionManager must not be null");
        this.transactionManager = transactionManager;
    }

    @Override
    public Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException {
        if (timeout >= 0) {
            this.transactionManager.setTransactionTimeout(timeout);
        }
        this.transactionManager.begin();
        return new ManagedTransactionAdapter(this.transactionManager);
    }

    @Override
    public boolean supportsResourceAdapterManagedTransactions() {
        return false;
    }

}
