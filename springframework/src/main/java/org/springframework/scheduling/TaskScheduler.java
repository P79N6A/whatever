package org.springframework.scheduling;

import org.springframework.lang.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

public interface TaskScheduler {

    @Nullable
    ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

    default ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
        return schedule(task, Date.from(startTime));
    }

    ScheduledFuture<?> schedule(Runnable task, Date startTime);

    default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
        return scheduleAtFixedRate(task, Date.from(startTime), period.toMillis());
    }

    ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period);

    default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
        return scheduleAtFixedRate(task, period.toMillis());
    }

    ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period);

    default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
        return scheduleWithFixedDelay(task, Date.from(startTime), delay.toMillis());
    }

    ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay);

    default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
        return scheduleWithFixedDelay(task, delay.toMillis());
    }

    ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay);

}
