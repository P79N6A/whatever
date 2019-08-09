package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

public interface TransactionAttributeSource {

    default boolean isCandidateClass(Class<?> targetClass) {
        return true;
    }

    @Nullable
    TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass);

}
