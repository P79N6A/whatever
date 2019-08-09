package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;

@SuppressWarnings("serial")
public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {

    @Nullable
    private String qualifier;

    @Nullable
    private String descriptor;

    public DefaultTransactionAttribute() {
        super();
    }

    public DefaultTransactionAttribute(TransactionAttribute other) {
        super(other);
    }

    public DefaultTransactionAttribute(int propagationBehavior) {
        super(propagationBehavior);
    }

    public void setQualifier(@Nullable String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    @Nullable
    public String getQualifier() {
        return this.qualifier;
    }

    public void setDescriptor(@Nullable String descriptor) {
        this.descriptor = descriptor;
    }

    @Nullable
    public String getDescriptor() {
        return this.descriptor;
    }

    @Override
    public boolean rollbackOn(Throwable ex) {
        return (ex instanceof RuntimeException || ex instanceof Error);
    }

    protected final StringBuilder getAttributeDescription() {
        StringBuilder result = getDefinitionDescription();
        if (StringUtils.hasText(this.qualifier)) {
            result.append("; '").append(this.qualifier).append("'");
        }
        return result;
    }

}
