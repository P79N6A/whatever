package org.springframework.transaction.jta;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import java.util.List;

public class JtaAfterCompletionSynchronization implements Synchronization {

    private final List<TransactionSynchronization> synchronizations;

    public JtaAfterCompletionSynchronization(List<TransactionSynchronization> synchronizations) {
        this.synchronizations = synchronizations;
    }

    @Override
    public void beforeCompletion() {
    }

    @Override
    public void afterCompletion(int status) {
        switch (status) {
            case Status.STATUS_COMMITTED:
                try {
                    TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
                } finally {
                    TransactionSynchronizationUtils.invokeAfterCompletion(this.synchronizations, TransactionSynchronization.STATUS_COMMITTED);
                }
                break;
            case Status.STATUS_ROLLEDBACK:
                TransactionSynchronizationUtils.invokeAfterCompletion(this.synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
                break;
            default:
                TransactionSynchronizationUtils.invokeAfterCompletion(this.synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
        }
    }

}
