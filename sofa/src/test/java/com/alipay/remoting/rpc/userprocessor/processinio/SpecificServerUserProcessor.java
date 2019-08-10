package com.alipay.remoting.rpc.userprocessor.processinio;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.NamedThreadFactory;
import com.alipay.remoting.rpc.common.RequestBody;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SpecificServerUserProcessor extends SyncUserProcessor<RequestBody> {

    private static final Logger logger = LoggerFactory.getLogger(SpecificServerUserProcessor.class);

    private long delayMs;

    private boolean delaySwitch;

    private AtomicInteger invokeTimes = new AtomicInteger();

    private AtomicInteger onewayTimes = new AtomicInteger();

    private AtomicInteger syncTimes = new AtomicInteger();

    private AtomicInteger futureTimes = new AtomicInteger();

    private AtomicInteger callbackTimes = new AtomicInteger();

    private ThreadPoolExecutor executor;

    private String remoteAddr;

    private CountDownLatch latch = new CountDownLatch(1);

    public SpecificServerUserProcessor() {
        this.delaySwitch = false;
        this.delayMs = 0;
        this.executor = new ThreadPoolExecutor(1, 3, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(4), new NamedThreadFactory("Rpc-common-executor"));
    }

    public SpecificServerUserProcessor(long delay) {
        this();
        if (delay < 0) {
            throw new IllegalArgumentException("delay time illegal!");
        }
        this.delaySwitch = true;
        this.delayMs = delay;
    }

    @Override
    public Object handleRequest(BizContext bizCtx, RequestBody request) throws Exception {
        String threadName = Thread.currentThread().getName();
        Assert.assertTrue(threadName.contains("Rpc-netty-server-worker"));
        logger.warn("Request received:" + request);
        this.remoteAddr = bizCtx.getRemoteAddress();
        long waittime = (Long) bizCtx.getInvokeContext().get(InvokeContext.BOLT_PROCESS_WAIT_TIME);
        logger.warn("Server User processor process wait time [" + waittime + "].");
        latch.countDown();
        logger.warn("Server User processor say, remote address is [" + this.remoteAddr + "].");
        Assert.assertEquals(RequestBody.class, request.getClass());
        processTimes(request);
        if (!delaySwitch) {
            return RequestBody.DEFAULT_SERVER_RETURN_STR;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return RequestBody.DEFAULT_SERVER_RETURN_STR;
    }

    private void processTimes(RequestBody req) {
        this.invokeTimes.incrementAndGet();
        if (req.getMsg().equals(RequestBody.DEFAULT_ONEWAY_STR)) {
            this.onewayTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_SYNC_STR)) {
            this.syncTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_FUTURE_STR)) {
            this.futureTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_CALLBACK_STR)) {
            this.callbackTimes.incrementAndGet();
        }
    }

    @Override
    public String interest() {
        return RequestBody.class.getName();
    }

    @Override
    public boolean processInIOThread() {
        return true;
    }

    public int getInvokeTimes() {
        return this.invokeTimes.get();
    }

    public int getInvokeTimesEachCallType(RequestBody.InvokeType type) {
        return new int[]{this.onewayTimes.get(), this.syncTimes.get(), this.futureTimes.get(), this.callbackTimes.get()}[type.ordinal()];
    }

    public String getRemoteAddr() throws InterruptedException {
        latch.await(100, TimeUnit.MILLISECONDS);
        return this.remoteAddr;
    }

}