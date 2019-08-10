package com.alipay.remoting.rpc.heartbeat;

import com.alipay.remoting.CommonCommandCode;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.config.Configs;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.common.*;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeClientHeartBeatTest {
    static Logger logger = LoggerFactory.getLogger(RuntimeClientHeartBeatTest.class);

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

    CustomHeartBeatProcessor heartBeatProcessor = new CustomHeartBeatProcessor();

    @Before
    public void init() {
        System.setProperty(Configs.TCP_IDLE, "100");
        System.setProperty(Configs.TCP_IDLE_SWITCH, Boolean.toString(true));
        System.setProperty(Configs.TCP_IDLE_MAXTIMES, "1000");
        server = new BoltServer(port);
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
    public void testRuntimeCloseAndEnableHeartbeat() throws InterruptedException {
        server.getRpcServer().registerProcessor(RpcProtocol.PROTOCOL_CODE, CommonCommandCode.HEARTBEAT, heartBeatProcessor);
        try {
            client.getConnection(addr, 1000);
        } catch (RemotingException e) {
            logger.error("", e);
        }
        Thread.sleep(1500);
        logger.warn("before disable: " + heartBeatProcessor.getHeartBeatTimes());
        Assert.assertTrue(heartBeatProcessor.getHeartBeatTimes() > 0);
        client.disableConnHeartbeat(addr);
        heartBeatProcessor.reset();
        Thread.sleep(1500);
        logger.warn("after disable: " + heartBeatProcessor.getHeartBeatTimes());
        Assert.assertTrue(heartBeatProcessor.getHeartBeatTimes() == 0);
        client.enableConnHeartbeat(addr);
        heartBeatProcessor.reset();
        Thread.sleep(1500);
        logger.warn("after enable: " + heartBeatProcessor.getHeartBeatTimes());
        Assert.assertTrue(heartBeatProcessor.getHeartBeatTimes() > 0);
    }

}
