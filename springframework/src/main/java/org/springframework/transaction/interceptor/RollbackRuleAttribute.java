package org.springframework.transaction.interceptor;

import org.springframework.util.Assert;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RollbackRuleAttribute implements Serializable {

    public static final RollbackRuleAttribute ROLLBACK_ON_RUNTIME_EXCEPTIONS = new RollbackRuleAttribute(RuntimeException.class);

    private final String exceptionName;

    public RollbackRuleAttribute(Class<?> clazz) {
        Assert.notNull(clazz, "'clazz' cannot be null");
        if (!Throwable.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Cannot construct rollback rule from [" + clazz.getName() + "]: it's not a Throwable");
        }
        this.exceptionName = clazz.getName();
    }

    public RollbackRuleAttribute(String exceptionName) {
        Assert.hasText(exceptionName, "'exceptionName' cannot be null or empty");
        this.exceptionName = exceptionName;
    }

    public String getExceptionName() {
        return this.exceptionName;
    }

    public int getDepth(Throwable ex) {
        return getDepth(ex.getClass(), 0);
    }

    private int getDepth(Class<?> exceptionClass, int depth) {
        if (exceptionClass.getName().contains(this.exceptionName)) {
            // Found it!
            return depth;
        }
        // If we've gone as far as we can go and haven't found it...
        if (exceptionClass == Throwable.class) {
            return -1;
        }
        return getDepth(exceptionClass.getSuperclass(), depth + 1);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RollbackRuleAttribute)) {
            return false;
        }
        RollbackRuleAttribute rhs = (RollbackRuleAttribute) other;
        return this.exceptionName.equals(rhs.exceptionName);
    }

    @Override
    public int hashCode() {
        return this.exceptionName.hashCode();
    }

    @Override
    public String toString() {
        return "RollbackRuleAttribute with pattern [" + this.exceptionName + "]";
    }

}
