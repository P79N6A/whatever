package org.springframework.transaction.support;

public class SimpleTransactionStatus extends AbstractTransactionStatus {

    private final boolean newTransaction;

    public SimpleTransactionStatus() {
        this(true);
    }

    public SimpleTransactionStatus(boolean newTransaction) {
        this.newTransaction = newTransaction;
    }

    @Override
    public boolean isNewTransaction() {
        return this.newTransaction;
    }

}
