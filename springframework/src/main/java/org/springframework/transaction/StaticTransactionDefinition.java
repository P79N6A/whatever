package org.springframework.transaction;

final class StaticTransactionDefinition implements TransactionDefinition {

    static final StaticTransactionDefinition INSTANCE = new StaticTransactionDefinition();

    private StaticTransactionDefinition() {
    }

}
