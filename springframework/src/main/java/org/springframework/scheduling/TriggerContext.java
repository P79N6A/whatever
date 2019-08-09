package org.springframework.scheduling;

import org.springframework.lang.Nullable;

import java.util.Date;

public interface TriggerContext {

    @Nullable
    Date lastScheduledExecutionTime();

    @Nullable
    Date lastActualExecutionTime();

    @Nullable
    Date lastCompletionTime();

}
