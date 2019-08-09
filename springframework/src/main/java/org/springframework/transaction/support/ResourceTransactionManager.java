package org.springframework.transaction.support;

import org.springframework.transaction.PlatformTransactionManager;

public interface ResourceTransactionManager extends PlatformTransactionManager {

    Object getResourceFactory();

}
