package com.alipay.remoting.rpc.userprocessor.executorselector;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.rpc.common.RequestBody;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class SpecificClientUserProcessor extends SyncUserProcessor<RequestBody> {

    private static final Logger logger = LoggerFactory.getLogger(SpecificClientUserProcessor.class);

    private long delayMs;

    private boolean delaySwitch;

    private AtomicInteger invokeTimes = new AtomicInteger();

    public SpecificClientUserProcessor() {
        this.delaySwitch = false;
        this.delayMs = 0;
    }

    public SpecificClientUserProcessor(long delay) {
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
        Assert.assertTrue(threadName.contains("Rpc-specific1-executor"));
        logger.warn("Request received:" + request);
        Assert.assertEquals(RequestBody.class, request.getClass());
        long waittime = (Long) bizCtx.getInvokeContext().get(InvokeContext.BOLT_PROCESS_WAIT_TIME);
        logger.warn("Client User processor process wait time [" + waittime + "].");
        invokeTimes.incrementAndGet();
        if (!delaySwitch) {
            return RequestBody.DEFAULT_CLIENT_RETURN_STR;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return RequestBody.DEFAULT_CLIENT_RETURN_STR;
    }

    @Override
    public String interest() {
        return RequestBody.class.getName();
    }

    public int getInvokeTimes() {
        return this.invokeTimes.get();
    }

}
