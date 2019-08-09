package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("serial")
public class CompositeTransactionAttributeSource implements TransactionAttributeSource, Serializable {

    private final TransactionAttributeSource[] transactionAttributeSources;

    public CompositeTransactionAttributeSource(TransactionAttributeSource... transactionAttributeSources) {
        Assert.notNull(transactionAttributeSources, "TransactionAttributeSource array must not be null");
        this.transactionAttributeSources = transactionAttributeSources;
    }

    public final TransactionAttributeSource[] getTransactionAttributeSources() {
        return this.transactionAttributeSources;
    }

    @Override
    public boolean isCandidateClass(Class<?> targetClass) {
        for (TransactionAttributeSource source : this.transactionAttributeSources) {
            if (source.isCandidateClass(targetClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        for (TransactionAttributeSource source : this.transactionAttributeSources) {
            TransactionAttribute attr = source.getTransactionAttribute(method, targetClass);
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

}
