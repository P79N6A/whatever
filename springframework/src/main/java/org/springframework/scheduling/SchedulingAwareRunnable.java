package org.springframework.scheduling;

public interface SchedulingAwareRunnable extends Runnable {

    boolean isLongLived();

}
