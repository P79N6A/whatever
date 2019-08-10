package com.alipay.remoting.rpc.userprocessor.multiinterestprocessor;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.NamedThreadFactory;
import com.alipay.remoting.rpc.common.RequestBody;
import com.alipay.remoting.rpc.protocol.SyncMutiInterestUserProcessor;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleClientMultiInterestUserProcessor extends SyncMutiInterestUserProcessor<MultiInterestBaseRequestBody> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleClientMultiInterestUserProcessor.class);

    private long delayMs;

    private boolean delaySwitch;

    private ThreadPoolExecutor executor;

    private boolean timeoutDiscard = true;

    private AtomicInteger c1invokeTimes = new AtomicInteger();

    private AtomicInteger c1onewayTimes = new AtomicInteger();

    private AtomicInteger c1syncTimes = new AtomicInteger();

    private AtomicInteger c1futureTimes = new AtomicInteger();

    private AtomicInteger c1callbackTimes = new AtomicInteger();

    private AtomicInteger c2invokeTimes = new AtomicInteger();

    private AtomicInteger c2onewayTimes = new AtomicInteger();

    private AtomicInteger c2syncTimes = new AtomicInteger();

    private AtomicInteger c2futureTimes = new AtomicInteger();

    private AtomicInteger c2callbackTimes = new AtomicInteger();

    public SimpleClientMultiInterestUserProcessor() {
        this.delaySwitch = false;
        this.delayMs = 0;
        this.executor = new ThreadPoolExecutor(1, 3, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(4), new NamedThreadFactory("Request-process-pool"));
    }

    public SimpleClientMultiInterestUserProcessor(long delay) {
        this();
        if (delay < 0) {
            throw new IllegalArgumentException("delay time illegal!");
        }
        this.delaySwitch = true;
        this.delayMs = delay;
    }

    public SimpleClientMultiInterestUserProcessor(long delay, int core, int max, int keepaliveSeconds, int workQueue) {
        this(delay);
        this.executor = new ThreadPoolExecutor(core, max, keepaliveSeconds, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(workQueue), new NamedThreadFactory("Request-process-pool"));
    }

    @Override
    public Object handleRequest(BizContext bizCtx, MultiInterestBaseRequestBody request) throws Exception {
        logger.warn("Request received:" + request);
        if (bizCtx.isRequestTimeout()) {
            String errMsg = "Stop process in client biz thread, already timeout!";
            logger.warn(errMsg);
            throw new Exception(errMsg);
        }
        if (request instanceof RequestBodyC1) {
            Assert.assertEquals(RequestBodyC1.class, request.getClass());
            return handleRequest(bizCtx, (RequestBodyC1) request);
        } else if (request instanceof RequestBodyC2) {
            Assert.assertEquals(RequestBodyC2.class, request.getClass());
            return handleRequest(bizCtx, (RequestBodyC2) request);
        } else {
            throw new Exception("RequestBody does not belong to defined interests !");
        }
    }

    private Object handleRequest(BizContext bizCtx, RequestBodyC1 request) {
        Long waittime = (Long) bizCtx.getInvokeContext().get(InvokeContext.BOLT_PROCESS_WAIT_TIME);
        Assert.assertNotNull(waittime);
        if (logger.isInfoEnabled()) {
            logger.info("Client User processor process wait time {}", waittime);
        }
        processTimes(request);
        if (!delaySwitch) {
            return RequestBodyC1.DEFAULT_CLIENT_RETURN_STR;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return RequestBodyC1.DEFAULT_CLIENT_RETURN_STR;
    }

    private Object handleRequest(BizContext bizCtx, RequestBodyC2 request) {
        Long waittime = (Long) bizCtx.getInvokeContext().get(InvokeContext.BOLT_PROCESS_WAIT_TIME);
        Assert.assertNotNull(waittime);
        if (logger.isInfoEnabled()) {
            logger.info("Client User processor process wait time {}", waittime);
        }
        processTimes(request);
        if (!delaySwitch) {
            return RequestBodyC2.DEFAULT_CLIENT_RETURN_STR;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return RequestBodyC2.DEFAULT_CLIENT_RETURN_STR;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public boolean timeoutDiscard() {
        return this.timeoutDiscard;
    }

    public int getInvokeTimesC1() {
        return this.c1invokeTimes.get();
    }

    public int getInvokeTimesC2() {
        return this.c2invokeTimes.get();
    }

    public int getInvokeTimesEachCallTypeC1(RequestBody.InvokeType type) {
        return new int[]{this.c1onewayTimes.get(), this.c1syncTimes.get(), this.c1futureTimes.get(), this.c1callbackTimes.get()}[type.ordinal()];
    }

    public int getInvokeTimesEachCallTypeC2(RequestBody.InvokeType type) {
        return new int[]{this.c2onewayTimes.get(), this.c2syncTimes.get(), this.c2futureTimes.get(), this.c2callbackTimes.get()}[type.ordinal()];
    }

    private void processTimes(RequestBodyC1 req) {
        this.c1invokeTimes.incrementAndGet();
        if (req.getMsg().equals(RequestBodyC1.DEFAULT_ONEWAY_STR)) {
            this.c1onewayTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBodyC1.DEFAULT_SYNC_STR)) {
            this.c1syncTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBodyC1.DEFAULT_FUTURE_STR)) {
            this.c1futureTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBodyC1.DEFAULT_CALLBACK_STR)) {
            this.c1callbackTimes.incrementAndGet();
        }
    }

    private void processTimes(RequestBodyC2 req) {
        this.c2invokeTimes.incrementAndGet();
        if (req.getMsg().equals(RequestBodyC2.DEFAULT_ONEWAY_STR)) {
            this.c2onewayTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBodyC2.DEFAULT_SYNC_STR)) {
            this.c2syncTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBodyC2.DEFAULT_FUTURE_STR)) {
            this.c2futureTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBodyC2.DEFAULT_CALLBACK_STR)) {
            this.c2callbackTimes.incrementAndGet();
        }
    }

    public boolean isTimeoutDiscard() {
        return timeoutDiscard;
    }

    public void setTimeoutDiscard(boolean timeoutDiscard) {
        this.timeoutDiscard = timeoutDiscard;
    }

    @Override
    public List<String> multiInterest() {
        List<String> list = new ArrayList<String>();
        list.add(RequestBodyC1.class.getName());
        list.add(RequestBodyC2.class.getName());
        return list;
    }

}