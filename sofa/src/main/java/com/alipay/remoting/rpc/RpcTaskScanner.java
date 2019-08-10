package com.alipay.remoting.rpc;

import com.alipay.remoting.AbstractLifeCycle;
import com.alipay.remoting.LifeCycleException;
import com.alipay.remoting.NamedThreadFactory;
import com.alipay.remoting.Scannable;
import com.alipay.remoting.log.BoltLoggerFactory;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcTaskScanner extends AbstractLifeCycle {

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    private final List<Scannable> scanList;

    private ScheduledExecutorService scheduledService;

    public RpcTaskScanner() {
        this.scanList = new LinkedList<Scannable>();
    }

    @Override
    public void startup() throws LifeCycleException {
        super.startup();
        scheduledService = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("RpcTaskScannerThread", true));
        scheduledService.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                for (Scannable scanned : scanList) {
                    try {
                        scanned.scan();
                    } catch (Throwable t) {
                        logger.error("Exception caught when scannings.", t);
                    }
                }
            }

        }, 10000, 10000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() throws LifeCycleException {
        super.shutdown();
        scheduledService.shutdown();
    }

    @Deprecated
    public void start() {
        startup();
    }

    public void add(Scannable target) {
        scanList.add(target);
    }

}
