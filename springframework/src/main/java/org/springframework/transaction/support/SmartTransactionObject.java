package org.springframework.transaction.support;

import java.io.Flushable;

public interface SmartTransactionObject extends Flushable {

    boolean isRollbackOnly();

    @Override
    void flush();

}
