package com.alipay.remoting.rpc.resrelease;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.BasicUsageTest;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.common.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClientStopTest {
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
        server = new BoltServer(port, true, true);
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

    @Test
    public void testRpcServerStop() throws InterruptedException {
        String connNumAddr = addr + "?_CONNECTIONNUM=8&_CONNECTIONWARMUP=true";
        try {
            client.getConnection(connNumAddr, 1000);
        } catch (RemotingException e) {
            logger.error("get connection exception!", e);
        }
        server.stop();
        Thread.sleep(3000);
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(8, serverConnectProcessor.getConnectTimes());
        Assert.assertTrue(serverDisConnectProcessor.isDisConnected());
        Assert.assertEquals(8, serverDisConnectProcessor.getDisConnectTimes());
        RequestBody req1 = new RequestBody(1, RequestBody.DEFAULT_CLIENT_STR);
        try {
            client.invokeSync(connNumAddr, req1, 1000);
            Assert.fail("Should not reach here, server should not be connected now!");
        } catch (RemotingException e) {
            logger.error("invoke sync failed!", e);
        }
    }

    @Test
    public void testRpcClientShutdown() throws InterruptedException {
        String connNumAddr = addr + "?_CONNECTIONNUM=8&_CONNECTIONWARMUP=true";
        try {
            client.getConnection(connNumAddr, 1000);
        } catch (RemotingException e) {
            logger.error("get connection exception!", e);
        }
        client.shutdown();
        Thread.sleep(1500);
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(8, serverConnectProcessor.getConnectTimes());
        Assert.assertTrue(serverDisConnectProcessor.isDisConnected());
        Assert.assertEquals(8, serverDisConnectProcessor.getDisConnectTimes());
    }

}
