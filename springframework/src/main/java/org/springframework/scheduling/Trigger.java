package org.springframework.scheduling;

import org.springframework.lang.Nullable;

import java.util.Date;

public interface Trigger {

    @Nullable
    Date nextExecutionTime(TriggerContext triggerContext);

}
