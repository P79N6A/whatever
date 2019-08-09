package org.springframework.scheduling.concurrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.util.concurrent.*;

@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory implements BeanNameAware, InitializingBean, DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    private ThreadFactory threadFactory = this;

    private boolean threadNamePrefixSet = false;

    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    private boolean waitForTasksToCompleteOnShutdown = false;

    private int awaitTerminationSeconds = 0;

    @Nullable
    private String beanName;

    @Nullable
    private ExecutorService executor;

    public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
        this.threadFactory = (threadFactory != null ? threadFactory : this);
    }

    @Override
    public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
        super.setThreadNamePrefix(threadNamePrefix);
        this.threadNamePrefixSet = true;
    }

    public void setRejectedExecutionHandler(@Nullable RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = (rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
    }

    public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
    }

    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void afterPropertiesSet() {
        initialize();
    }

    public void initialize() {
        if (logger.isInfoEnabled()) {
            logger.info("Initializing ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
        }
        if (!this.threadNamePrefixSet && this.beanName != null) {
            setThreadNamePrefix(this.beanName + "-");
        }
        this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
    }

    protected abstract ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);

    @Override
    public void destroy() {
        shutdown();
    }

    public void shutdown() {
        if (logger.isInfoEnabled()) {
            logger.info("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
        }
        if (this.executor != null) {
            if (this.waitForTasksToCompleteOnShutdown) {
                this.executor.shutdown();
            } else {
                for (Runnable remainingTask : this.executor.shutdownNow()) {
                    cancelRemainingTask(remainingTask);
                }
            }
            awaitTerminationIfNecessary(this.executor);
        }
    }

    protected void cancelRemainingTask(Runnable task) {
        if (task instanceof Future) {
            ((Future<?>) task).cancel(true);
        }
    }

    private void awaitTerminationIfNecessary(ExecutorService executor) {
        if (this.awaitTerminationSeconds > 0) {
            try {
                if (!executor.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS)) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Timed out while waiting for executor" + (this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
                    }
                }
            } catch (InterruptedException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Interrupted while waiting for executor" + (this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
                }
                Thread.currentThread().interrupt();
            }
        }
    }

}
