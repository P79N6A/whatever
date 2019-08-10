package com.alipay.remoting.rpc.watermark;

import com.alipay.remoting.Connection;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.common.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WaterMark_UserProperty_ExceptionTest {
    static Logger logger = LoggerFactory.getLogger(WaterMark_UserProperty_ExceptionTest.class);

    BoltServer server;

    RpcClient client;

    int port = PortScan.select();

    String ip = "127.0.0.1";

    String addr = "127.0.0.1:" + port;

    int invokeTimes = 10;

    SimpleServerUserProcessor serverUserProcessor = new SimpleServerUserProcessor(0, 20, 20, 60, 100);

    SimpleClientUserProcessor clientUserProcessor = new SimpleClientUserProcessor(0, 20, 20, 60, 100);

    CONNECTEventProcessor clientConnectProcessor = new CONNECTEventProcessor();

    CONNECTEventProcessor serverConnectProcessor = new CONNECTEventProcessor();

    DISCONNECTEventProcessor clientDisConnectProcessor = new DISCONNECTEventProcessor();

    DISCONNECTEventProcessor serverDisConnectProcessor = new DISCONNECTEventProcessor();

    @Before
    public void init() {
        server = new BoltServer(port, true);
        server.getRpcServer().initWriteBufferWaterMark(1, 2);
        server.start();
        server.addConnectionEventProcessor(ConnectionEventType.CONNECT, serverConnectProcessor);
        server.addConnectionEventProcessor(ConnectionEventType.CLOSE, serverDisConnectProcessor);
        server.registerUserProcessor(serverUserProcessor);
        client = new RpcClient();
        client.addConnectionEventProcessor(ConnectionEventType.CONNECT, clientConnectProcessor);
        client.addConnectionEventProcessor(ConnectionEventType.CLOSE, clientDisConnectProcessor);
        client.registerUserProcessor(clientUserProcessor);
        client.initWriteBufferWaterMark(1, 2);
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
    public void testSync() throws InterruptedException {
        final RequestBody req = new RequestBody(1, 1024);
        final List<Boolean> overFlow = new ArrayList<Boolean>();
        for (int i = 0; i < invokeTimes; i++) {
            new Thread() {
                @Override
                public void run() {
                    String res = null;
                    try {
                        for (int i = 0; i < invokeTimes; i++) {
                            res = (String) client.invokeSync(addr, req, 3000);
                        }
                    } catch (RemotingException e) {
                        if (e.getMessage().contains("overflow")) {
                            logger.error("overflow exception!");
                            overFlow.add(true);
                        }
                    } catch (InterruptedException e) {
                        String errMsg = "InterruptedException caught in sync!";
                        logger.error(errMsg, e);
                        Assert.fail(errMsg);
                    }
                    logger.warn("Result received in sync: " + res);
                    Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR, res);
                }
            }.start();
        }
        Thread.sleep(3000);
        if (overFlow.size() > 0 && overFlow.get(0)) {
            Assert.assertTrue(serverConnectProcessor.isConnected());
            Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
            Assert.assertTrue(invokeTimes * invokeTimes > serverUserProcessor.getInvokeTimes());
        } else {
            Assert.fail("Should not reach here");
        }
    }

    @Test
    public void testServerSyncUsingConnection() throws Exception {
        Connection clientConn = client.createStandaloneConnection(ip, port, 1000);
        RequestBody req1 = new RequestBody(1, RequestBody.DEFAULT_CLIENT_STR);
        String serverres = (String) client.invokeSync(clientConn, req1, 1000);
        Assert.assertEquals(serverres, RequestBody.DEFAULT_SERVER_RETURN_STR);
        final String remoteAddr = serverUserProcessor.getRemoteAddr();
        Assert.assertNotNull(remoteAddr);
        final List<Boolean> overFlow = new ArrayList<Boolean>();
        final RequestBody req = new RequestBody(1, 1024);
        for (int i = 0; i < invokeTimes; i++) {
            new Thread() {
                public void run() {
                    try {
                        for (int i = 0; i < invokeTimes; i++) {
                            String clientres = (String) server.getRpcServer().invokeSync(remoteAddr, req, 1000);
                            Assert.assertEquals(clientres, RequestBody.DEFAULT_CLIENT_RETURN_STR);
                        }
                    } catch (RemotingException e) {
                        if (e.getMessage().contains("overflow")) {
                            logger.error("overflow exception!");
                            overFlow.add(true);
                        }
                    } catch (InterruptedException e) {
                        String errMsg = "InterruptedException caught in sync!";
                        logger.error(errMsg, e);
                        Assert.fail(errMsg);
                    }

                }
            }.start();
        }
        Thread.sleep(3000);
        if (overFlow.size() > 0 && overFlow.get(0)) {
            Assert.assertTrue(serverConnectProcessor.isConnected());
            Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
            Assert.assertTrue(invokeTimes * invokeTimes > clientUserProcessor.getInvokeTimes());
        } else {
            Assert.fail("Should not reach here");
        }
    }

}