package com.alipay.remoting.rpc.connectionmanage;

import com.alipay.remoting.Connection;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.RemotingAddressParser;
import com.alipay.remoting.Url;
import com.alipay.remoting.config.Configs;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.DefaultInvokeFuture;
import com.alipay.remoting.rpc.RpcAddressParser;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcCommandType;
import com.alipay.remoting.rpc.common.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ScheduledDisconnectStrategyTest {

    BoltServer server;

    RpcClient client;

    int port = 2018;

    SimpleServerUserProcessor serverUserProcessor = new SimpleServerUserProcessor();

    SimpleClientUserProcessor clientUserProcessor = new SimpleClientUserProcessor();

    CONNECTEventProcessor clientConnectProcessor = new CONNECTEventProcessor();

    CONNECTEventProcessor serverConnectProcessor = new CONNECTEventProcessor();

    DISCONNECTEventProcessor clientDisConnectProcessor = new DISCONNECTEventProcessor();

    DISCONNECTEventProcessor serverDisConnectProcessor = new DISCONNECTEventProcessor();

    private RemotingAddressParser addressParser = new RpcAddressParser();

    @Before
    public void init() {
    }

    @After
    public void stop() {
        client.shutdown();
        server.stop();
    }

    @Test
    public void testConnectionMonitorBySystemSetting() throws InterruptedException, RemotingException {
        System.setProperty(Configs.CONN_MONITOR_INITIAL_DELAY, "2000");
        System.setProperty(Configs.CONN_MONITOR_PERIOD, "100");
        doInit(true, false);
        String addr = "127.0.0.1:" + port + "?zone=RZONE&_CONNECTIONNUM=8&_CONNECTIONWARMUP=false";
        Url url = addressParser.parse(addr);
        for (int i = 0; i < 8; ++i) {
            client.getConnection(url, 1000);
        }
        Thread.sleep(2150);
        Assert.assertTrue(1 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertEquals(9, clientConnectProcessor.getConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(2 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertTrue(9 <= clientConnectProcessor.getConnectTimes());
        Thread.sleep(400);
        Assert.assertTrue(4 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertTrue(9 <= clientConnectProcessor.getConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(100);
        Assert.assertTrue(6 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertTrue(10 <= clientConnectProcessor.getConnectTimes());
    }

    @Test
    public void testConnectionMonitorByUserSetting() throws InterruptedException, RemotingException {
        System.setProperty(Configs.CONN_MONITOR_INITIAL_DELAY, "2000");
        System.setProperty(Configs.CONN_MONITOR_PERIOD, "100");
        doInit(false, true);
        String addr = "127.0.0.1:" + port + "?zone=RZONE&_CONNECTIONNUM=8&_CONNECTIONWARMUP=false";
        Url url = addressParser.parse(addr);
        for (int i = 0; i < 8; ++i) {
            client.getConnection(url, 1000);
        }
        Thread.sleep(2150);
        Assert.assertTrue(1 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertEquals(9, clientConnectProcessor.getConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(2 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertTrue(9 <= clientConnectProcessor.getConnectTimes());
        Thread.sleep(400);
        Assert.assertTrue(4 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertTrue(9 <= clientConnectProcessor.getConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(100);
        Assert.assertTrue(6 <= clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertTrue(10 <= clientConnectProcessor.getConnectTimes());
    }

    @Test
    public void testCloseFreshSelectConnections_bySystemSetting() throws RemotingException, InterruptedException {
        System.setProperty(Configs.RETRY_DETECT_PERIOD, "500");
        System.setProperty(Configs.CONN_MONITOR_INITIAL_DELAY, "2000");
        System.setProperty(Configs.CONN_MONITOR_PERIOD, "100");
        System.setProperty(Configs.CONN_THRESHOLD, "0");
        doInit(true, false);
        String addr = "127.0.0.1:" + port + "?zone=RZONE&_CONNECTIONNUM=1";
        Url url = addressParser.parse(addr);
        final Connection connection = client.getConnection(url, 1000);
        connection.addInvokeFuture(new DefaultInvokeFuture(1, null, null, RpcCommandType.REQUEST, null));
        Thread.sleep(2100);
        Assert.assertTrue(0 == clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertEquals(1, clientConnectProcessor.getConnectTimes());
        connection.removeInvokeFuture(1);
        Thread.sleep(100);
        Assert.assertTrue(0 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(500);
        Assert.assertTrue(0 <= clientDisConnectProcessor.getDisConnectTimes());
    }

    @Test
    public void testCloseFreshSelectConnections_byUserSetting() throws RemotingException, InterruptedException {
        System.setProperty(Configs.RETRY_DETECT_PERIOD, "500");
        System.setProperty(Configs.CONN_MONITOR_INITIAL_DELAY, "2000");
        System.setProperty(Configs.CONN_MONITOR_PERIOD, "100");
        System.setProperty(Configs.CONN_THRESHOLD, "0");
        doInit(false, true);
        String addr = "127.0.0.1:" + port + "?zone=RZONE&_CONNECTIONNUM=1";
        Url url = addressParser.parse(addr);
        final Connection connection = client.getConnection(url, 1000);
        connection.addInvokeFuture(new DefaultInvokeFuture(1, null, null, RpcCommandType.REQUEST, null));
        Thread.sleep(2100);
        Assert.assertTrue(0 == clientDisConnectProcessor.getDisConnectTimes());
        Assert.assertEquals(1, clientConnectProcessor.getConnectTimes());
        connection.removeInvokeFuture(1);
        Thread.sleep(100);
        Assert.assertEquals(1, clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(500);
        Assert.assertTrue(0 <= clientDisConnectProcessor.getDisConnectTimes());
    }

    @Test
    public void testDisconnectStrategy_bySystemSetting() throws InterruptedException, RemotingException {
        System.setProperty(Configs.CONN_MONITOR_SWITCH, "true");
        System.setProperty(Configs.CONN_MONITOR_INITIAL_DELAY, "2000");
        System.setProperty(Configs.CONN_MONITOR_PERIOD, "100");
        System.setProperty(Configs.CONN_THRESHOLD, "0");
        doInit(true, false);
        String addr = "127.0.0.1:" + port + "?zone=RZONE&_CONNECTIONNUM=8";
        Url url = addressParser.parse(addr);
        for (int i = 0; i < 8; i++) {
            client.getConnection(url, 1000);
        }
        Thread.sleep(2100);
        Assert.assertTrue(0 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(2 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(400);
        Assert.assertTrue(4 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(100);
        Assert.assertTrue(6 <= clientDisConnectProcessor.getDisConnectTimes());
    }

    @Test
    public void testDisconnectStrategy_byUserSetting() throws InterruptedException, RemotingException {
        System.setProperty(Configs.CONN_MONITOR_SWITCH, "true");
        System.setProperty(Configs.CONN_MONITOR_INITIAL_DELAY, "2000");
        System.setProperty(Configs.CONN_MONITOR_PERIOD, "100");
        System.setProperty(Configs.CONN_THRESHOLD, "0");
        doInit(false, true);
        String addr = "127.0.0.1:" + port + "?zone=RZONE&_CONNECTIONNUM=8";
        Url url = addressParser.parse(addr);
        for (int i = 0; i < 8; i++) {
            client.getConnection(url, 1000);
        }
        Thread.sleep(2100);
        Assert.assertTrue(0 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(2 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(400);
        Assert.assertTrue(4 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(200);
        Assert.assertTrue(5 <= clientDisConnectProcessor.getDisConnectTimes());
        Thread.sleep(100);
        Assert.assertTrue(6 <= clientDisConnectProcessor.getDisConnectTimes());
    }

    private void doInit(boolean enableSystem, boolean enableUser) {
        if (enableSystem) {
            System.setProperty(Configs.CONN_MONITOR_SWITCH, "true");
            System.setProperty(Configs.CONN_RECONNECT_SWITCH, "true");
        } else {
            System.setProperty(Configs.CONN_MONITOR_SWITCH, "false");
            System.setProperty(Configs.CONN_RECONNECT_SWITCH, "false");
        }
        server = new BoltServer(port, false, true);
        server.start();
        server.addConnectionEventProcessor(ConnectionEventType.CONNECT, serverConnectProcessor);
        server.addConnectionEventProcessor(ConnectionEventType.CLOSE, serverDisConnectProcessor);
        server.registerUserProcessor(serverUserProcessor);
        client = new RpcClient();
        if (enableUser) {
            client.enableReconnectSwitch();
            client.enableConnectionMonitorSwitch();
        }
        client.addConnectionEventProcessor(ConnectionEventType.CONNECT, clientConnectProcessor);
        client.addConnectionEventProcessor(ConnectionEventType.CLOSE, clientDisConnectProcessor);
        client.registerUserProcessor(clientUserProcessor);
        client.init();
    }

}
