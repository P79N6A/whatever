package org.springframework.transaction.interceptor;

import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@SuppressWarnings("serial")
public class TransactionAttributeSourceAdvisor extends AbstractPointcutAdvisor {

    @Nullable
    private TransactionInterceptor transactionInterceptor;

    private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
        @Override
        @Nullable
        protected TransactionAttributeSource getTransactionAttributeSource() {
            return (transactionInterceptor != null ? transactionInterceptor.getTransactionAttributeSource() : null);
        }
    };

    public TransactionAttributeSourceAdvisor() {
    }

    public TransactionAttributeSourceAdvisor(TransactionInterceptor interceptor) {
        setTransactionInterceptor(interceptor);
    }

    public void setTransactionInterceptor(TransactionInterceptor interceptor) {
        this.transactionInterceptor = interceptor;
    }

    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }

    @Override
    public Advice getAdvice() {
        Assert.state(this.transactionInterceptor != null, "No TransactionInterceptor set");
        return this.transactionInterceptor;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

}
