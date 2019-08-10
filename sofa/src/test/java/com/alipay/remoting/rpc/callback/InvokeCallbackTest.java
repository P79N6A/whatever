package com.alipay.remoting.rpc.callback;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.InvokeCallback;
import com.alipay.remoting.rpc.BasicUsageTest;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.common.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InvokeCallbackTest {
    static Logger logger = LoggerFactory.getLogger(BasicUsageTest.class);

    BoltServer server;

    RpcClient client;

    int port = PortScan.select();

    String ip = "127.0.0.1";

    String addr = "127.0.0.1:" + port;

    int invokeTimes = 5;

    SimpleServerUserProcessor serverUserProcessor = new SimpleServerUserProcessor();

    SimpleClientUserProcessor clientUserProcessor = new SimpleClientUserProcessor();

    CONNECTEventProcessor clientConnectProcessor = new CONNECTEventProcessor();

    CONNECTEventProcessor serverConnectProcessor = new CONNECTEventProcessor();

    DISCONNECTEventProcessor clientDisConnectProcessor = new DISCONNECTEventProcessor();

    DISCONNECTEventProcessor serverDisConnectProcessor = new DISCONNECTEventProcessor();

    @Before
    public void init() {
        server = new BoltServer(port, true);
        server.start();
        server.addConnectionEventProcessor(ConnectionEventType.CONNECT, serverConnectProcessor);
        server.addConnectionEventProcessor(ConnectionEventType.CLOSE, serverDisConnectProcessor);
        server.registerUserProcessor(serverUserProcessor);
        client = new RpcClient();
        client.addConnectionEventProcessor(ConnectionEventType.CONNECT, clientConnectProcessor);
        client.addConnectionEventProcessor(ConnectionEventType.CLOSE, clientDisConnectProcessor);
        client.registerUserProcessor(clientUserProcessor);
        client.init();
    }

    @After
    public void stop() {
        try {
            server.stop();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.error("Stop server failed!", e);
        }
    }

    @Test
    public void testCallbackInvokeTimes() {
        RequestBody req = new RequestBody(1, "hello world sync");
        final AtomicInteger callbackInvokTimes = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            client.invokeWithCallback(addr, req, new InvokeCallback() {
                Executor executor = Executors.newCachedThreadPool();

                @Override
                public void onResponse(Object result) {
                    callbackInvokTimes.getAndIncrement();
                    latch.countDown();
                    throw new RuntimeException("Hehe Exception");
                }

                @Override
                public void onException(Throwable e) {
                    callbackInvokTimes.getAndIncrement();
                    latch.countDown();
                    throw new RuntimeException("Hehe Exception");
                }

                @Override
                public Executor getExecutor() {
                    return executor;
                }
            }, 3000);
            Thread.sleep(500);
            if (latch.await(3000, TimeUnit.MILLISECONDS)) {
                Assert.assertEquals(1, callbackInvokTimes.get());
            }
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

}