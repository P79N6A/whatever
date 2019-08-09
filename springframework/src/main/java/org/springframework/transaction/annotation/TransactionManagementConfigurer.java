package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionManager;

public interface TransactionManagementConfigurer {

    TransactionManager annotationDrivenTransactionManager();

}
