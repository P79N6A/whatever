package org.springframework.transaction.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

class ApplicationListenerMethodTransactionalAdapter extends ApplicationListenerMethodAdapter {

    private final TransactionalEventListener annotation;

    public ApplicationListenerMethodTransactionalAdapter(String beanName, Class<?> targetClass, Method method) {
        super(beanName, targetClass, method);
        TransactionalEventListener ann = AnnotatedElementUtils.findMergedAnnotation(method, TransactionalEventListener.class);
        if (ann == null) {
            throw new IllegalStateException("No TransactionalEventListener annotation found on method: " + method);
        }
        this.annotation = ann;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronization transactionSynchronization = createTransactionSynchronization(event);
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
        } else if (this.annotation.fallbackExecution()) {
            if (this.annotation.phase() == TransactionPhase.AFTER_ROLLBACK && logger.isWarnEnabled()) {
                logger.warn("Processing " + event + " as a fallback execution on AFTER_ROLLBACK phase");
            }
            processEvent(event);
        } else {
            // No transactional event execution at all
            if (logger.isDebugEnabled()) {
                logger.debug("No transaction is active - skipping " + event);
            }
        }
    }

    private TransactionSynchronization createTransactionSynchronization(ApplicationEvent event) {
        return new TransactionSynchronizationEventAdapter(this, event, this.annotation.phase());
    }

    private static class TransactionSynchronizationEventAdapter extends TransactionSynchronizationAdapter {

        private final ApplicationListenerMethodAdapter listener;

        private final ApplicationEvent event;

        private final TransactionPhase phase;

        public TransactionSynchronizationEventAdapter(ApplicationListenerMethodAdapter listener, ApplicationEvent event, TransactionPhase phase) {
            this.listener = listener;
            this.event = event;
            this.phase = phase;
        }

        @Override
        public int getOrder() {
            return this.listener.getOrder();
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            if (this.phase == TransactionPhase.BEFORE_COMMIT) {
                processEvent();
            }
        }

        @Override
        public void afterCompletion(int status) {
            if (this.phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) {
                processEvent();
            } else if (this.phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK) {
                processEvent();
            } else if (this.phase == TransactionPhase.AFTER_COMPLETION) {
                processEvent();
            }
        }

        protected void processEvent() {
            this.listener.processEvent(this.event);
        }

    }

}
