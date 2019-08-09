package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

public interface CallbackPreferringPlatformTransactionManager extends PlatformTransactionManager {

    @Nullable
    <T> T execute(@Nullable TransactionDefinition definition, TransactionCallback<T> callback) throws TransactionException;

}
