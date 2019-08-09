package org.springframework.transaction.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.dao.support.PersistenceExceptionTranslator;

@SuppressWarnings("serial")
abstract class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

    protected TransactionAttributeSourcePointcut() {
        setClassFilter(new TransactionAttributeSourceClassFilter());
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        TransactionAttributeSource tas = getTransactionAttributeSource();
        return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransactionAttributeSourcePointcut)) {
            return false;
        }
        TransactionAttributeSourcePointcut otherPc = (TransactionAttributeSourcePointcut) other;
        return ObjectUtils.nullSafeEquals(getTransactionAttributeSource(), otherPc.getTransactionAttributeSource());
    }

    @Override
    public int hashCode() {
        return TransactionAttributeSourcePointcut.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getTransactionAttributeSource();
    }

    @Nullable
    protected abstract TransactionAttributeSource getTransactionAttributeSource();

    private class TransactionAttributeSourceClassFilter implements ClassFilter {

        @Override
        public boolean matches(Class<?> clazz) {
            if (TransactionalProxy.class.isAssignableFrom(clazz) || PlatformTransactionManager.class.isAssignableFrom(clazz)
                    || PersistenceExceptionTranslator.class.isAssignableFrom(clazz)
            ) {
                return false;
            }
            TransactionAttributeSource tas = getTransactionAttributeSource();
            return (tas == null || tas.isCandidateClass(clazz));
        }

    }

}
