package mmp.threadpool;

import mmp.Future;

import java.util.concurrent.Delayed;

public interface ScheduledFuture<V> extends Delayed, Future<V> {
}
