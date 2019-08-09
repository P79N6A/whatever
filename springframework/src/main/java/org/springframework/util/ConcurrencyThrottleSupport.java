package org.springframework.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class ConcurrencyThrottleSupport implements Serializable {

    public static final int UNBOUNDED_CONCURRENCY = -1;

    public static final int NO_CONCURRENCY = 0;

    protected transient Log logger = LogFactory.getLog(getClass());

    private transient Object monitor = new Object();

    private int concurrencyLimit = UNBOUNDED_CONCURRENCY;

    private int concurrencyCount = 0;

    public void setConcurrencyLimit(int concurrencyLimit) {
        this.concurrencyLimit = concurrencyLimit;
    }

    public int getConcurrencyLimit() {
        return this.concurrencyLimit;
    }

    public boolean isThrottleActive() {
        return (this.concurrencyLimit >= 0);
    }

    protected void beforeAccess() {
        if (this.concurrencyLimit == NO_CONCURRENCY) {
            throw new IllegalStateException("Currently no invocations allowed - concurrency limit set to NO_CONCURRENCY");
        }
        if (this.concurrencyLimit > 0) {
            boolean debug = logger.isDebugEnabled();
            synchronized (this.monitor) {
                boolean interrupted = false;
                while (this.concurrencyCount >= this.concurrencyLimit) {
                    if (interrupted) {
                        throw new IllegalStateException("Thread was interrupted while waiting for invocation access, " + "but concurrency limit still does not allow for entering");
                    }
                    if (debug) {
                        logger.debug("Concurrency count " + this.concurrencyCount + " has reached limit " + this.concurrencyLimit + " - blocking");
                    }
                    try {
                        this.monitor.wait();
                    } catch (InterruptedException ex) {
                        // Re-interrupt current thread, to allow other threads to react.
                        Thread.currentThread().interrupt();
                        interrupted = true;
                    }
                }
                if (debug) {
                    logger.debug("Entering throttle at concurrency count " + this.concurrencyCount);
                }
                this.concurrencyCount++;
            }
        }
    }

    protected void afterAccess() {
        if (this.concurrencyLimit >= 0) {
            synchronized (this.monitor) {
                this.concurrencyCount--;
                if (logger.isDebugEnabled()) {
                    logger.debug("Returning from throttle at concurrency count " + this.concurrencyCount);
                }
                this.monitor.notify();
            }
        }
    }
    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization, just initialize state after deserialization.
        ois.defaultReadObject();
        // Initialize transient fields.
        this.logger = LogFactory.getLog(getClass());
        this.monitor = new Object();
    }

}
