package com.alipay.remoting.rpc.lock;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcAddressParser;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.common.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class CreateConnLockTest {

    static Logger logger = LoggerFactory.getLogger(CreateConnLockTest.class);

    BoltServer server;

    RpcClient client;

    int port = 12200;

    String ip = "127.0.0.1";

    String bad_ip = "127.0.0.2";

    String ip_prefix = "127.0.0.";

    String addr = "127.0.0.1:" + port;

    int invokeTimes = 3;

    ConcurrentServerUserProcessor serverUserProcessor = new ConcurrentServerUserProcessor();

    SimpleClientUserProcessor clientUserProcessor = new SimpleClientUserProcessor();

    CONNECTEventProcessor clientConnectProcessor = new CONNECTEventProcessor();

    CONNECTEventProcessor serverConnectProcessor = new CONNECTEventProcessor();

    DISCONNECTEventProcessor clientDisConnectProcessor = new DISCONNECTEventProcessor();

    DISCONNECTEventProcessor serverDisConnectProcessor = new DISCONNECTEventProcessor();

    private AtomicBoolean whetherConnectTimeoutConsumedTooLong = new AtomicBoolean();

    @Before
    public void init() {
        whetherConnectTimeoutConsumedTooLong.set(false);
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
    public void testSync_DiffAddressOnePort() throws InterruptedException {
        for (int i = 0; i < invokeTimes; ++i) {
            Url url = new Url(ip_prefix + i, port);
            MyThread thread = new MyThread(url, 1, false);
            new Thread(thread).start();
        }
        Thread.sleep(5000);
        Assert.assertFalse(whetherConnectTimeoutConsumedTooLong.get());
    }

    @Test
    public void testSync_OneAddressDiffPort() throws InterruptedException {
        for (int i = 0; i < invokeTimes; ++i) {
            Url url = new Url(ip, port++);
            MyThread thread = new MyThread(url, 1, false);
            new Thread(thread).start();
        }
        Thread.sleep(5000);
        Assert.assertFalse(whetherConnectTimeoutConsumedTooLong.get());
    }

    @Test
    public void testSync_OneAddressOnePort() throws InterruptedException {
        for (int i = 0; i < invokeTimes; ++i) {
            Url url = new Url(bad_ip, port);
            MyThread thread = new MyThread(url, 1, false);
            new Thread(thread).start();
        }
        Thread.sleep(5000);
        Assert.assertFalse(whetherConnectTimeoutConsumedTooLong.get());
    }

    class MyThread implements Runnable {
        Url url;

        int connNum;

        boolean warmup;

        RpcAddressParser parser;

        public MyThread(Url url, int connNum, boolean warmup) {
            this.url = url;
            this.connNum = connNum;
            this.warmup = warmup;
            this.parser = new RpcAddressParser();
        }

        @Override
        public void run() {
            InvokeContext ctx = new InvokeContext();
            try {
                RequestBody req = new RequestBody(1, "hello world sync");
                url.setConnectTimeout(100);
                url.setConnNum(connNum);
                url.setConnWarmup(warmup);
                this.parser.initUrlArgs(url);
                client.invokeSync(url, req, ctx, 3000);
                long time = getAndPrintCreateConnTime(ctx);

            } catch (RemotingException e) {
                logger.error("error!", e);
                long time = getAndPrintCreateConnTime(ctx);

            } catch (Exception e) {
                logger.error("error!", e);
                long time = getAndPrintCreateConnTime(ctx);

            }
        }

        private long getAndPrintCreateConnTime(InvokeContext ctx) {
            long time = ctx.get(InvokeContext.CLIENT_CONN_CREATETIME) == null ? -1l : (Long) ctx.get(InvokeContext.CLIENT_CONN_CREATETIME);
            if (time > 1500) {
                whetherConnectTimeoutConsumedTooLong.set(true);
            }
            logger.warn("CREATE CONN TIME CONSUMED: " + time);
            return time;
        }

    }

}