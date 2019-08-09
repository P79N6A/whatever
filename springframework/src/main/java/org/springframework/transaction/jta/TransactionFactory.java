package org.springframework.transaction.jta;

import org.springframework.lang.Nullable;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

public interface TransactionFactory {

    Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException;

    boolean supportsResourceAdapterManagedTransactions();

}
