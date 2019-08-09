package org.springframework.transaction.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.*;
import org.springframework.util.Assert;

import java.lang.reflect.UndeclaredThrowableException;

@SuppressWarnings("serial")
public class TransactionTemplate extends DefaultTransactionDefinition implements TransactionOperations, InitializingBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private PlatformTransactionManager transactionManager;

    public TransactionTemplate() {
    }

    public TransactionTemplate(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public TransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
        super(transactionDefinition);
        this.transactionManager = transactionManager;
    }

    public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Nullable
    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.transactionManager == null) {
            throw new IllegalArgumentException("Property 'transactionManager' is required");
        }
    }

    @Override
    @Nullable
    public <T> T execute(TransactionCallback<T> action) throws TransactionException {
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
        if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager) {
            return ((CallbackPreferringPlatformTransactionManager) this.transactionManager).execute(this, action);
        } else {
            TransactionStatus status = this.transactionManager.getTransaction(this);
            T result;
            try {
                result = action.doInTransaction(status);
            } catch (RuntimeException | Error ex) {
                // Transactional code threw application exception -> rollback
                rollbackOnException(status, ex);
                throw ex;
            } catch (Throwable ex) {
                // Transactional code threw unexpected exception -> rollback
                rollbackOnException(status, ex);
                throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
            }
            this.transactionManager.commit(status);
            return result;
        }
    }

    private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
        logger.debug("Initiating transaction rollback on application exception", ex);
        try {
            this.transactionManager.rollback(status);
        } catch (TransactionSystemException ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            ex2.initApplicationException(ex);
            throw ex2;
        } catch (RuntimeException | Error ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            throw ex2;
        }
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (super.equals(other) && (!(other instanceof TransactionTemplate) || getTransactionManager() == ((TransactionTemplate) other).getTransactionManager())));
    }

}
