package com.alipay.remoting.rpc.watermark;

import com.alipay.remoting.config.Configs;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.common.BoltServer;
import com.alipay.remoting.rpc.common.PortScan;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WaterMarkInitTest {
    BoltServer server;

    RpcClient client;

    int port = PortScan.select();

    String addr = "127.0.0.1:" + port;

    @Before
    public void init() {
    }

    @After
    public void stop() {
    }

    @Test
    public void testLowBiggerThanHigh() {
        System.setProperty(Configs.NETTY_BUFFER_HIGH_WATERMARK, Integer.toString(1));
        System.setProperty(Configs.NETTY_BUFFER_LOW_WATERMARK, Integer.toString(2));
        try {
            server = new BoltServer(port, true);
            server.start();
            Assert.fail("should not reach here");
        } catch (IllegalStateException e) {
        }
        try {
            client = new RpcClient();
            client.init();
            Assert.fail("should not reach here");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testLowBiggerThanDefaultHigh() throws InterruptedException {
        System.setProperty(Configs.NETTY_BUFFER_HIGH_WATERMARK, Integer.toString(300 * 1024));
        System.setProperty(Configs.NETTY_BUFFER_LOW_WATERMARK, Integer.toString(200 * 1024));
        server = new BoltServer(port, true);
        Assert.assertTrue(server.start());
        try {
            client = new RpcClient();
            client.init();
            client.getConnection(addr, 3000);
        } catch (IllegalArgumentException e) {
            Assert.fail("should not reach here");
        } catch (RemotingException e) {
        }
    }

    @Test
    public void testHighSmallerThanDefaultLow() throws InterruptedException {
        System.setProperty(Configs.NETTY_BUFFER_HIGH_WATERMARK, Integer.toString(3 * 1024));
        System.setProperty(Configs.NETTY_BUFFER_LOW_WATERMARK, Integer.toString(2 * 1024));
        server = new BoltServer(port, true);
        Assert.assertTrue(server.start());
        try {
            client = new RpcClient();
            client.init();
            client.getConnection(addr, 3000);
        } catch (IllegalArgumentException e) {
            Assert.fail("should not reach here");
        } catch (RemotingException e) {
        }
    }

}