package org.springframework.scheduling.concurrent;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class ForkJoinPoolFactoryBean implements FactoryBean<ForkJoinPool>, InitializingBean, DisposableBean {

    private boolean commonPool = false;

    private int parallelism = Runtime.getRuntime().availableProcessors();

    private ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

    @Nullable
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private boolean asyncMode = false;

    private int awaitTerminationSeconds = 0;

    @Nullable
    private ForkJoinPool forkJoinPool;

    public void setCommonPool(boolean commonPool) {
        this.commonPool = commonPool;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public void setThreadFactory(ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public void setAsyncMode(boolean asyncMode) {
        this.asyncMode = asyncMode;
    }

    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    @Override
    public void afterPropertiesSet() {
        this.forkJoinPool = (this.commonPool ? ForkJoinPool.commonPool() : new ForkJoinPool(this.parallelism, this.threadFactory, this.uncaughtExceptionHandler, this.asyncMode));
    }

    @Override
    @Nullable
    public ForkJoinPool getObject() {
        return this.forkJoinPool;
    }

    @Override
    public Class<?> getObjectType() {
        return ForkJoinPool.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() {
        if (this.forkJoinPool != null) {
            // Ignored for the common pool.
            this.forkJoinPool.shutdown();
            // Wait for all tasks to terminate - works for the common pool as well.
            if (this.awaitTerminationSeconds > 0) {
                try {
                    this.forkJoinPool.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
