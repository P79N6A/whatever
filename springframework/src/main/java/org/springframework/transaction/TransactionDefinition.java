package org.springframework.transaction;

import org.springframework.lang.Nullable;

public interface TransactionDefinition {

    int PROPAGATION_REQUIRED = 0;

    int PROPAGATION_SUPPORTS = 1;

    int PROPAGATION_MANDATORY = 2;

    int PROPAGATION_REQUIRES_NEW = 3;

    int PROPAGATION_NOT_SUPPORTED = 4;

    int PROPAGATION_NEVER = 5;

    int PROPAGATION_NESTED = 6;

    int ISOLATION_DEFAULT = -1;

    int ISOLATION_READ_UNCOMMITTED = 1;  // same as java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;

    int ISOLATION_READ_COMMITTED = 2;  // same as java.sql.Connection.TRANSACTION_READ_COMMITTED;

    int ISOLATION_REPEATABLE_READ = 4;  // same as java.sql.Connection.TRANSACTION_REPEATABLE_READ;

    int ISOLATION_SERIALIZABLE = 8;  // same as java.sql.Connection.TRANSACTION_SERIALIZABLE;

    int TIMEOUT_DEFAULT = -1;

    default int getPropagationBehavior() {
        return PROPAGATION_REQUIRED;
    }

    default int getIsolationLevel() {
        return ISOLATION_DEFAULT;
    }

    default int getTimeout() {
        return TIMEOUT_DEFAULT;
    }

    default boolean isReadOnly() {
        return false;
    }

    @Nullable
    default String getName() {
        return null;
    }
    // Static builder methods

    static TransactionDefinition withDefaults() {
        return StaticTransactionDefinition.INSTANCE;
    }

}
