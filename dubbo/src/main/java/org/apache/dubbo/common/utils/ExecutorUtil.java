package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.*;

import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;

public class ExecutorUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtil.class);

    private static final ThreadPoolExecutor SHUTDOWN_EXECUTOR = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100), new NamedThreadFactory("Close-ExecutorService-Timer", true));

    public static boolean isTerminated(Executor executor) {
        if (executor instanceof ExecutorService) {
            if (((ExecutorService) executor).isTerminated()) {
                return true;
            }
        }
        return false;
    }

    public static void gracefulShutdown(Executor executor, int timeout) {
        if (!(executor instanceof ExecutorService) || isTerminated(executor)) {
            return;
        }
        final ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdown();
        } catch (SecurityException ex2) {
            return;
        } catch (NullPointerException ex2) {
            return;
        }
        try {
            if (!es.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                es.shutdownNow();
            }
        } catch (InterruptedException ex) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (!isTerminated(es)) {
            newThreadToCloseExecutor(es);
        }
    }

    public static void shutdownNow(Executor executor, final int timeout) {
        if (!(executor instanceof ExecutorService) || isTerminated(executor)) {
            return;
        }
        final ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdownNow();
        } catch (SecurityException ex2) {
            return;
        } catch (NullPointerException ex2) {
            return;
        }
        try {
            es.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (!isTerminated(es)) {
            newThreadToCloseExecutor(es);
        }
    }

    private static void newThreadToCloseExecutor(final ExecutorService es) {
        if (!isTerminated(es)) {
            SHUTDOWN_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 1000; i++) {
                            es.shutdownNow();
                            if (es.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                                break;
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            });
        }
    }

    public static URL setThreadName(URL url, String defaultName) {
        String name = url.getParameter(THREAD_NAME_KEY, defaultName);
        name = name + "-" + url.getAddress();
        url = url.addParameter(THREAD_NAME_KEY, name);
        return url;
    }

    public static void cancelScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        ScheduledFuture<?> future = scheduledFuture;
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
    }

}
