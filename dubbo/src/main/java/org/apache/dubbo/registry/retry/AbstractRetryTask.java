package org.apache.dubbo.registry.retry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.RegistryConstants.*;

public abstract class AbstractRetryTask implements TimerTask {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final URL url;

    protected final FailbackRegistry registry;

    final long retryPeriod;

    private final int retryTimes;

    private final String taskName;

    private int times = 1;

    private volatile boolean cancel;

    AbstractRetryTask(URL url, FailbackRegistry registry, String taskName) {
        if (url == null || StringUtils.isBlank(taskName)) {
            throw new IllegalArgumentException();
        }
        this.url = url;
        this.registry = registry;
        this.taskName = taskName;
        cancel = false;
        this.retryPeriod = url.getParameter(REGISTRY_RETRY_PERIOD_KEY, DEFAULT_REGISTRY_RETRY_PERIOD);
        this.retryTimes = url.getParameter(REGISTRY_RETRY_TIMES_KEY, DEFAULT_REGISTRY_RETRY_TIMES);
    }

    public void cancel() {
        cancel = true;
    }

    public boolean isCancel() {
        return cancel;
    }

    protected void reput(Timeout timeout, long tick) {
        if (timeout == null) {
            throw new IllegalArgumentException();
        }
        Timer timer = timeout.timer();
        if (timer.isStop() || timeout.isCancelled() || isCancel()) {
            return;
        }
        times++;
        timer.newTimeout(timeout.task(), tick, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (timeout.isCancelled() || timeout.timer().isStop() || isCancel()) {
            return;
        }
        if (times > retryTimes) {
            logger.warn("Final failed to execute task " + taskName + ", url: " + url + ", retry " + retryTimes + " times.");
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info(taskName + " : " + url);
        }
        try {
            doRetry(url, registry, timeout);
        } catch (Throwable t) {
            logger.warn("Failed to execute task " + taskName + ", url: " + url + ", waiting for again, cause:" + t.getMessage(), t);
            reput(timeout, retryPeriod);
        }
    }

    protected abstract void doRetry(URL url, FailbackRegistry registry, Timeout timeout);

}
