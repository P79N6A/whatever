package org.springframework.scheduling.concurrent;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.util.concurrent.*;

@SuppressWarnings("serial")
public class ThreadPoolExecutorFactoryBean extends ExecutorConfigurationSupport implements FactoryBean<ExecutorService>, InitializingBean, DisposableBean {

    private int corePoolSize = 1;

    private int maxPoolSize = Integer.MAX_VALUE;

    private int keepAliveSeconds = 60;

    private boolean allowCoreThreadTimeOut = false;

    private int queueCapacity = Integer.MAX_VALUE;

    private boolean exposeUnconfigurableExecutor = false;

    @Nullable
    private ExecutorService exposedExecutor;

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
        this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
    }

    @Override
    protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
        ThreadPoolExecutor executor = createExecutor(this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, queue, threadFactory, rejectedExecutionHandler);
        if (this.allowCoreThreadTimeOut) {
            executor.allowCoreThreadTimeOut(true);
        }
        // Wrap executor with an unconfigurable decorator.
        this.exposedExecutor = (this.exposeUnconfigurableExecutor ? Executors.unconfigurableExecutorService(executor) : executor);
        return executor;
    }

    protected ThreadPoolExecutor createExecutor(int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);
    }

    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
        if (queueCapacity > 0) {
            return new LinkedBlockingQueue<>(queueCapacity);
        } else {
            return new SynchronousQueue<>();
        }
    }

    @Override
    @Nullable
    public ExecutorService getObject() {
        return this.exposedExecutor;
    }

    @Override
    public Class<? extends ExecutorService> getObjectType() {
        return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ExecutorService.class);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
