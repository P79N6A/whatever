package org.springframework.scheduling.support;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PeriodicTrigger implements Trigger {

    private final long period;

    private final TimeUnit timeUnit;

    private volatile long initialDelay = 0;

    private volatile boolean fixedRate = false;

    public PeriodicTrigger(long period) {
        this(period, null);
    }

    public PeriodicTrigger(long period, @Nullable TimeUnit timeUnit) {
        Assert.isTrue(period >= 0, "period must not be negative");
        this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
        this.period = this.timeUnit.toMillis(period);
    }

    public long getPeriod() {
        return this.period;
    }

    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = this.timeUnit.toMillis(initialDelay);
    }

    public long getInitialDelay() {
        return this.initialDelay;
    }

    public void setFixedRate(boolean fixedRate) {
        this.fixedRate = fixedRate;
    }

    public boolean isFixedRate() {
        return this.fixedRate;
    }

    @Override
    public Date nextExecutionTime(TriggerContext triggerContext) {
        Date lastExecution = triggerContext.lastScheduledExecutionTime();
        Date lastCompletion = triggerContext.lastCompletionTime();
        if (lastExecution == null || lastCompletion == null) {
            return new Date(System.currentTimeMillis() + this.initialDelay);
        }
        if (this.fixedRate) {
            return new Date(lastExecution.getTime() + this.period);
        }
        return new Date(lastCompletion.getTime() + this.period);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PeriodicTrigger)) {
            return false;
        }
        PeriodicTrigger otherTrigger = (PeriodicTrigger) other;
        return (this.fixedRate == otherTrigger.fixedRate && this.initialDelay == otherTrigger.initialDelay && this.period == otherTrigger.period);
    }

    @Override
    public int hashCode() {
        return (this.fixedRate ? 17 : 29) + (int) (37 * this.period) + (int) (41 * this.initialDelay);
    }

}
