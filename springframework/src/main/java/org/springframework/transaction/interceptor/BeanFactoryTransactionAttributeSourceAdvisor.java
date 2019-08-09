package org.springframework.transaction.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.lang.Nullable;

/**
 * Advisor
 */
@SuppressWarnings("serial")
public class BeanFactoryTransactionAttributeSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    @Nullable
    private TransactionAttributeSource transactionAttributeSource;

    private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
        @Override
        @Nullable
        protected TransactionAttributeSource getTransactionAttributeSource() {
            return transactionAttributeSource;
        }
    };

    public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
        this.transactionAttributeSource = transactionAttributeSource;
    }

    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

}
