package com.alipay.remoting.rpc.prehandle;

import com.alipay.remoting.*;
import com.alipay.remoting.rpc.common.RequestBody;
import com.alipay.remoting.rpc.common.SimpleServerUserProcessor;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PreHandleUserProcessor extends SyncUserProcessor<RequestBody> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleServerUserProcessor.class);

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 3, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(4), new NamedThreadFactory("Request-process-pool"));

    private AtomicInteger invokeTimes = new AtomicInteger();

    @Override
    public BizContext preHandleRequest(RemotingContext remotingCtx, RequestBody request) {
        BizContext ctx = new MyBizContext(remotingCtx);
        ctx.put("test", "test");
        return ctx;
    }

    @Override
    public Object handleRequest(BizContext bizCtx, RequestBody request) throws Exception {
        logger.warn("Request received:" + request);
        invokeTimes.incrementAndGet();
        long waittime = (Long) bizCtx.getInvokeContext().get(InvokeContext.BOLT_PROCESS_WAIT_TIME);
        logger.warn("PreHandleUserProcessor User processor process wait time [" + waittime + "].");
        Assert.assertEquals(RequestBody.class, request.getClass());
        Assert.assertEquals("127.0.0.1", bizCtx.getRemoteHost());
        Assert.assertTrue(bizCtx.getRemotePort() != -1);
        return bizCtx.get("test");
    }

    @Override
    public String interest() {
        return RequestBody.class.getName();
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    public int getInvokeTimes() {
        return this.invokeTimes.get();
    }

    class MyBizContext extends DefaultBizContext implements BizContext {

        private Map<String, String> custCtx = new HashMap<String, String>();

        public MyBizContext(RemotingContext remotingCtx) {
            super(remotingCtx);
        }

        @Override
        public void put(String key, String value) {
            custCtx.put(key, value);
        }

        @Override
        public String get(String key) {
            return custCtx.get(key);
        }

    }

}
