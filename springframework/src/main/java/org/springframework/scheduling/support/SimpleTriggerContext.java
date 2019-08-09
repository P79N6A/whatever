package org.springframework.scheduling.support;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TriggerContext;

import java.util.Date;

public class SimpleTriggerContext implements TriggerContext {

    @Nullable
    private volatile Date lastScheduledExecutionTime;

    @Nullable
    private volatile Date lastActualExecutionTime;

    @Nullable
    private volatile Date lastCompletionTime;

    public SimpleTriggerContext() {
    }

    public SimpleTriggerContext(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
        this.lastScheduledExecutionTime = lastScheduledExecutionTime;
        this.lastActualExecutionTime = lastActualExecutionTime;
        this.lastCompletionTime = lastCompletionTime;
    }

    public void update(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
        this.lastScheduledExecutionTime = lastScheduledExecutionTime;
        this.lastActualExecutionTime = lastActualExecutionTime;
        this.lastCompletionTime = lastCompletionTime;
    }

    @Override
    @Nullable
    public Date lastScheduledExecutionTime() {
        return this.lastScheduledExecutionTime;
    }

    @Override
    @Nullable
    public Date lastActualExecutionTime() {
        return this.lastActualExecutionTime;
    }

    @Override
    @Nullable
    public Date lastCompletionTime() {
        return this.lastCompletionTime;
    }

}
