package mmp;

public  interface RunnableScheduledFuture<V> extends RunnableFuture<V>, ScheduledFuture<V> {

    boolean isPeriodic();
}