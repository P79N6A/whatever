package org.springframework.transaction;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@SuppressWarnings("serial")
public class TransactionSystemException extends TransactionException {

    @Nullable
    private Throwable applicationException;

    public TransactionSystemException(String msg) {
        super(msg);
    }

    public TransactionSystemException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public void initApplicationException(Throwable ex) {
        Assert.notNull(ex, "Application exception must not be null");
        if (this.applicationException != null) {
            throw new IllegalStateException("Already holding an application exception: " + this.applicationException);
        }
        this.applicationException = ex;
    }

    @Nullable
    public final Throwable getApplicationException() {
        return this.applicationException;
    }

    @Nullable
    public Throwable getOriginalException() {
        return (this.applicationException != null ? this.applicationException : getCause());
    }

    @Override
    public boolean contains(@Nullable Class<?> exType) {
        return super.contains(exType) || (exType != null && exType.isInstance(this.applicationException));
    }

}
