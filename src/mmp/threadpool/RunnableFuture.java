package mmp.threadpool;

import mmp.Future;

public interface RunnableFuture<V> extends Runnable, Future<V> {

    void run();
}
