package org.springframework.transaction.support;

import org.springframework.transaction.TransactionDefinition;

public interface ResourceTransactionDefinition extends TransactionDefinition {

    boolean isLocalResource();

}
