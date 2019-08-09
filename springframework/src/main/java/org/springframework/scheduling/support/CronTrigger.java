package org.springframework.scheduling.support;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import java.util.Date;
import java.util.TimeZone;

public class CronTrigger implements Trigger {

    private final CronSequenceGenerator sequenceGenerator;

    public CronTrigger(String expression) {
        this.sequenceGenerator = new CronSequenceGenerator(expression);
    }

    public CronTrigger(String expression, TimeZone timeZone) {
        this.sequenceGenerator = new CronSequenceGenerator(expression, timeZone);
    }

    public String getExpression() {
        return this.sequenceGenerator.getExpression();
    }

    @Override
    public Date nextExecutionTime(TriggerContext triggerContext) {
        Date date = triggerContext.lastCompletionTime();
        if (date != null) {
            Date scheduled = triggerContext.lastScheduledExecutionTime();
            if (scheduled != null && date.before(scheduled)) {
                // Previous task apparently executed too early...
                // Let's simply use the last calculated execution time then,
                // in order to prevent accidental re-fires in the same second.
                date = scheduled;
            }
        } else {
            date = new Date();
        }
        return this.sequenceGenerator.next(date);
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof CronTrigger && this.sequenceGenerator.equals(((CronTrigger) other).sequenceGenerator)));
    }

    @Override
    public int hashCode() {
        return this.sequenceGenerator.hashCode();
    }

    @Override
    public String toString() {
        return this.sequenceGenerator.toString();
    }

}
