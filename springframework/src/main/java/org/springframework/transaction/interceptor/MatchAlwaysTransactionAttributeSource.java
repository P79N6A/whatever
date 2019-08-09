package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("serial")
public class MatchAlwaysTransactionAttributeSource implements TransactionAttributeSource, Serializable {

    private TransactionAttribute transactionAttribute = new DefaultTransactionAttribute();

    public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
        this.transactionAttribute = transactionAttribute;
    }

    @Override
    @Nullable
    public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        return (ClassUtils.isUserLevelMethod(method) ? this.transactionAttribute : null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MatchAlwaysTransactionAttributeSource)) {
            return false;
        }
        MatchAlwaysTransactionAttributeSource otherTas = (MatchAlwaysTransactionAttributeSource) other;
        return ObjectUtils.nullSafeEquals(this.transactionAttribute, otherTas.transactionAttribute);
    }

    @Override
    public int hashCode() {
        return MatchAlwaysTransactionAttributeSource.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + this.transactionAttribute;
    }

}
