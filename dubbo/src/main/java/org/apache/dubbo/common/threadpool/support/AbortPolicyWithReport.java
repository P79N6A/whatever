package org.apache.dubbo.common.threadpool.support;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JVMUtil;

import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;

/**
 * Abort Policy.
 * Log warn info when abort.
 */
public class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy {

    protected static final Logger logger = LoggerFactory.getLogger(AbortPolicyWithReport.class);

    private final String threadName;

    private final URL url;

    private static volatile long lastPrintTime = 0;

    private static final long TEN_MINUTES_MILLS = 10 * 60 * 1000;

    private static final String OS_WIN_PREFIX = "win";

    private static final String OS_NAME_KEY = "os.name";

    private static final String WIN_DATETIME_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd_HH:mm:ss";

    private static Semaphore guard = new Semaphore(1);

    public AbortPolicyWithReport(String threadName, URL url) {
        this.threadName = threadName;
        this.url = url;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        String msg = String.format("Thread pool is EXHAUSTED!" + " Thread Name: %s, Pool Size: %d (active: %d, core: %d, max: %d, largest: %d), Task: %d (completed: " + "%d)," + " Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s), in %s://%s:%d!", threadName, e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(), e.getLargestPoolSize(), e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating(), url.getProtocol(), url.getIp(), url.getPort());
        logger.warn(msg);
        dumpJStack();
        throw new RejectedExecutionException(msg);
    }

    private void dumpJStack() {
        long now = System.currentTimeMillis();
        //dump every 10 minutes
        if (now - lastPrintTime < TEN_MINUTES_MILLS) {
            return;
        }
        if (!guard.tryAcquire()) {
            return;
        }
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.execute(() -> {
            String dumpPath = url.getParameter(DUMP_DIRECTORY, System.getProperty("user.home"));
            SimpleDateFormat sdf;
            String os = System.getProperty(OS_NAME_KEY).toLowerCase();
            // window system don't support ":" in file name
            if (os.contains(OS_WIN_PREFIX)) {
                sdf = new SimpleDateFormat(WIN_DATETIME_FORMAT);
            } else {
                sdf = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT);
            }
            String dateStr = sdf.format(new Date());
            //try-with-resources
            try (FileOutputStream jStackStream = new FileOutputStream(new File(dumpPath, "Dubbo_JStack.log" + "." + dateStr))) {
                JVMUtil.jstack(jStackStream);
            } catch (Throwable t) {
                logger.error("dump jStack error", t);
            } finally {
                guard.release();
            }
            lastPrintTime = System.currentTimeMillis();
        });
        //must shutdown thread pool ,if not will lead to OOM
        pool.shutdown();

    }

}
