package com.alipay.remoting.rpc;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.InvokeCallback;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.common.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerBasicUsageTest {
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
    public void testOneway() throws InterruptedException, RemotingException {
        client.getConnection(addr, 1000);
        RequestBody req = new RequestBody(1, RequestBody.DEFAULT_SERVER_STR);
        for (int i = 0; i < invokeTimes; i++) {
            try {
                String remoteAddr = serverUserProcessor.getRemoteAddr();
                Assert.assertNull(remoteAddr);
                remoteAddr = serverConnectProcessor.getRemoteAddr();
                Assert.assertNotNull(remoteAddr);
                server.getRpcServer().oneway(remoteAddr, req);
                Thread.sleep(100);
            } catch (RemotingException e) {
                String errMsg = "RemotingException caught in oneway!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
        }
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(0, serverUserProcessor.getInvokeTimes());
        Assert.assertEquals(invokeTimes, clientUserProcessor.getInvokeTimes());
    }

    @Test
    public void testSync() throws InterruptedException, RemotingException {
        client.getConnection(addr, 1000);
        RequestBody req = new RequestBody(1, RequestBody.DEFAULT_SERVER_STR);
        for (int i = 0; i < invokeTimes; i++) {
            try {
                String remoteAddr = serverUserProcessor.getRemoteAddr();
                Assert.assertNull(remoteAddr);
                remoteAddr = serverConnectProcessor.getRemoteAddr();
                Assert.assertNotNull(remoteAddr);
                String clientres = (String) server.getRpcServer().invokeSync(remoteAddr, req, 1000);
                Assert.assertEquals(clientres, RequestBody.DEFAULT_CLIENT_RETURN_STR);
            } catch (RemotingException e) {
                String errMsg = "RemotingException caught in sync!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
        }
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(0, serverUserProcessor.getInvokeTimes());
        Assert.assertEquals(invokeTimes, clientUserProcessor.getInvokeTimes());
    }

    @Test
    public void testFuture() throws InterruptedException, RemotingException {
        client.getConnection(addr, 1000);
        RequestBody req = new RequestBody(1, RequestBody.DEFAULT_SERVER_STR);
        for (int i = 0; i < invokeTimes; i++) {
            try {
                String remoteAddr = serverUserProcessor.getRemoteAddr();
                Assert.assertNull(remoteAddr);
                remoteAddr = serverConnectProcessor.getRemoteAddr();
                Assert.assertNotNull(remoteAddr);
                RpcResponseFuture future = server.getRpcServer().invokeWithFuture(remoteAddr, req, 1000);
                String clientres = (String) future.get(1000);
                Assert.assertEquals(clientres, RequestBody.DEFAULT_CLIENT_RETURN_STR);
            } catch (RemotingException e) {
                String errMsg = "RemotingException caught in future!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
        }
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(0, serverUserProcessor.getInvokeTimes());
        Assert.assertEquals(invokeTimes, clientUserProcessor.getInvokeTimes());
    }

    @Test
    public void testCallback() throws InterruptedException, RemotingException {
        client.getConnection(addr, 1000);
        RequestBody req = new RequestBody(1, RequestBody.DEFAULT_SERVER_STR);
        for (int i = 0; i < invokeTimes; i++) {
            try {
                String remoteAddr = serverUserProcessor.getRemoteAddr();
                Assert.assertNull(remoteAddr);
                remoteAddr = serverConnectProcessor.getRemoteAddr();
                Assert.assertNotNull(remoteAddr);
                final List<String> rets = new ArrayList<String>(1);
                final CountDownLatch latch = new CountDownLatch(1);
                server.getRpcServer().invokeWithCallback(remoteAddr, req, new InvokeCallback() {
                    Executor executor = Executors.newCachedThreadPool();

                    @Override
                    public void onResponse(Object result) {
                        logger.warn("Result received in callback: " + result);
                        rets.add((String) result);
                        latch.countDown();
                    }

                    @Override
                    public void onException(Throwable e) {
                        logger.error("Process exception in callback.", e);
                        latch.countDown();
                    }

                    @Override
                    public Executor getExecutor() {
                        return executor;
                    }
                }, 1000);
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    String errMsg = "InterruptedException caught in callback!";
                    logger.error(errMsg, e);
                    Assert.fail(errMsg);
                }
                if (rets.size() == 0) {
                    Assert.fail("No result! Maybe exception caught!");
                }
                Assert.assertEquals(RequestBody.DEFAULT_CLIENT_RETURN_STR, rets.get(0));
                rets.clear();
            } catch (RemotingException e) {
                String errMsg = "RemotingException caught in oneway!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
        }
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(0, serverUserProcessor.getInvokeTimes());
        Assert.assertEquals(invokeTimes, clientUserProcessor.getInvokeTimes());
    }

}