package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.DelegatingTransactionDefinition;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class DelegatingTransactionAttribute extends DelegatingTransactionDefinition implements TransactionAttribute, Serializable {

    private final TransactionAttribute targetAttribute;

    public DelegatingTransactionAttribute(TransactionAttribute targetAttribute) {
        super(targetAttribute);
        this.targetAttribute = targetAttribute;
    }

    @Override
    @Nullable
    public String getQualifier() {
        return this.targetAttribute.getQualifier();
    }

    @Override
    public boolean rollbackOn(Throwable ex) {
        return this.targetAttribute.rollbackOn(ex);
    }

}
