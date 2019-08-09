package org.springframework.util.backoff;

@FunctionalInterface
public interface BackOffExecution {

    long STOP = -1;

    long nextBackOff();

}
