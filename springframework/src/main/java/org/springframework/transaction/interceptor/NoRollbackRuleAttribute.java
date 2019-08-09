package org.springframework.transaction.interceptor;

@SuppressWarnings("serial")
public class NoRollbackRuleAttribute extends RollbackRuleAttribute {

    public NoRollbackRuleAttribute(Class<?> clazz) {
        super(clazz);
    }

    public NoRollbackRuleAttribute(String exceptionName) {
        super(exceptionName);
    }

    @Override
    public String toString() {
        return "No" + super.toString();
    }

}
