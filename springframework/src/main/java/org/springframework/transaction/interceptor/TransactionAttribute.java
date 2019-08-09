package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;

public interface TransactionAttribute extends TransactionDefinition {

    @Nullable
    String getQualifier();

    boolean rollbackOn(Throwable ex);

}
