package com.alipay.remoting.rpc.timeout;

import com.alipay.remoting.Connection;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.InvokeCallback;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcResponseFuture;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.common.*;
import com.alipay.remoting.rpc.exception.InvokeTimeoutException;
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

public class ServerTimeoutTest {
    static Logger logger = LoggerFactory.getLogger(ServerTimeoutTest.class);

    BoltServer server;

    RpcClient client;

    int port = PortScan.select();

    String ip = "127.0.0.1";

    String addr = "127.0.0.1:" + port;

    int invokeTimes = 5;

    int max_timeout = 500;

    int coreThread = 1;

    int maxThread = 1;

    int workQueue = 1;

    int concurrent = maxThread + workQueue;

    SimpleServerUserProcessor serverUserProcessor = new SimpleServerUserProcessor(max_timeout, coreThread, maxThread, 60, workQueue);

    SimpleClientUserProcessor clientUserProcessor = new SimpleClientUserProcessor(max_timeout, coreThread, maxThread, 60, workQueue);

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
        server.stop();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.error("Stop server failed!", e);
        }
    }

    @Test
    public void testOneway() {
        for (int i = 0; i <= 1; ++i) {
            new Thread() {
                @Override
                public void run() {
                    oneway(client, null);
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(2, serverUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.ONEWAY));
    }

    @Test
    public void testServerOneway() {
        for (int i = 0; i <= 1; ++i) {
            new Thread() {
                @Override
                public void run() {
                    oneway(client, server.getRpcServer());
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(2, clientUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.ONEWAY));
    }

    @Test
    public void testSync() {
        final int timeout[] = {max_timeout / 2, max_timeout / 3};
        for (int i = 0; i <= 1; ++i) {
            final int j = i;
            new Thread() {
                @Override
                public void run() {
                    sync(client, null, timeout[j]);
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(1, serverUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.SYNC));
    }

    @Test
    public void testServerSync() {
        final int timeout[] = {max_timeout / 2, max_timeout / 3};
        for (int i = 0; i <= 1; ++i) {
            final int j = i;
            new Thread() {
                @Override
                public void run() {
                    sync(client, server.getRpcServer(), timeout[j]);
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(1, clientUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.SYNC));
    }

    @Test
    public void testFuture() {
        final int timeout[] = {max_timeout / 2, max_timeout / 3};
        for (int i = 0; i <= 1; ++i) {
            final int j = i;
            new Thread() {
                @Override
                public void run() {
                    future(client, null, timeout[j]);
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(1, serverUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.FUTURE));
    }

    @Test
    public void testServerFuture() {
        final int timeout[] = {max_timeout / 2, max_timeout / 3};
        for (int i = 0; i <= 1; ++i) {
            final int j = i;
            new Thread() {
                @Override
                public void run() {
                    future(client, server.getRpcServer(), timeout[j]);
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(1, clientUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.FUTURE));
    }

    @Test
    public void testCallBack() {
        final int timeout[] = {max_timeout / 2, max_timeout / 3};
        for (int i = 0; i <= 1; ++i) {
            final int j = i;
            new Thread() {
                @Override
                public void run() {
                    callback(client, null, timeout[j]);
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(1, serverUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.CALLBACK));
    }

    @Test
    public void testServerCallBack() {
        final int timeout[] = {max_timeout / 2, max_timeout / 3};
        for (int i = 0; i <= 1; ++i) {
            final int j = i;
            new Thread() {
                @Override
                public void run() {
                    callback(client, server.getRpcServer(), timeout[j]);
                }
            }.start();
        }
        try {
            Thread.sleep(max_timeout * 2);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        Assert.assertEquals(1, clientUserProcessor.getInvokeTimesEachCallType(RequestBody.InvokeType.CALLBACK));
    }

    private void oneway(RpcClient client, RpcServer server) {
        RequestBody b2 = new RequestBody(2, RequestBody.DEFAULT_ONEWAY_STR);
        try {
            if (null == server) {
                client.oneway(addr, b2);
            } else {
                Connection conn = client.getConnection(addr, 1000);
                Assert.assertNotNull(serverConnectProcessor.getConnection());
                Connection serverConn = serverConnectProcessor.getConnection();
                server.oneway(serverConn, b2);
            }
            Thread.sleep(50);
        } catch (RemotingException e) {
            logger.error("Exception caught in oneway!", e);
            Assert.fail("Exception caught!");
        } catch (InterruptedException e) {
            logger.error("InterruptedException in oneway", e);
            Assert.fail("Should not reach here!");
        }
    }

    private void sync(RpcClient client, RpcServer server, int timeout) {
        RequestBody b1 = new RequestBody(1, RequestBody.DEFAULT_SYNC_STR);
        Object obj = null;
        try {
            if (null == server) {
                obj = client.invokeSync(addr, b1, timeout);
            } else {
                Connection conn = client.getConnection(addr, timeout);
                Assert.assertNotNull(serverConnectProcessor.getConnection());
                Connection serverConn = serverConnectProcessor.getConnection();
                obj = server.invokeSync(serverConn, b1, timeout);
            }
            Assert.fail("Should not reach here!");
        } catch (InvokeTimeoutException e) {
            Assert.assertNull(obj);
        } catch (RemotingException e) {
            logger.error("Other RemotingException but RpcServerTimeoutException occurred in sync", e);
            Assert.fail("Should not reach here!");
        } catch (InterruptedException e) {
            logger.error("InterruptedException in sync", e);
            Assert.fail("Should not reach here!");
        }
    }

    private void future(RpcClient client, RpcServer server, int timeout) {
        RequestBody b1 = new RequestBody(1, RequestBody.DEFAULT_FUTURE_STR);
        Object obj = null;
        try {
            RpcResponseFuture future = null;
            if (null == server) {
                future = client.invokeWithFuture(addr, b1, timeout);
            } else {
                Connection conn = client.getConnection(addr, timeout);
                Assert.assertNotNull(serverConnectProcessor.getConnection());
                Connection serverConn = serverConnectProcessor.getConnection();
                future = server.invokeWithFuture(serverConn, b1, timeout);
            }
            obj = future.get(timeout);
            Assert.fail("Should not reach here!");
        } catch (InvokeTimeoutException e) {
            Assert.assertNull(obj);
        } catch (RemotingException e) {
            logger.error("Other RemotingException but RpcServerTimeoutException occurred in sync", e);
            Assert.fail("Should not reach here!");
        } catch (InterruptedException e) {
            logger.error("InterruptedException in sync", e);
            Assert.fail("Should not reach here!");
        }
    }

    private void callback(RpcClient client, RpcServer server, int timeout) {
        RequestBody b1 = new RequestBody(1, RequestBody.DEFAULT_CALLBACK_STR);
        final List<String> rets = new ArrayList<String>(1);
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            if (null == server) {
                client.invokeWithCallback(addr, b1, new InvokeCallback() {
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
                        rets.add(e.getClass().getName());
                        latch.countDown();
                    }

                    @Override
                    public Executor getExecutor() {
                        return executor;
                    }

                }, timeout);
            } else {
                Connection conn = client.getConnection(addr, timeout);
                Assert.assertNotNull(serverConnectProcessor.getConnection());
                Connection serverConn = serverConnectProcessor.getConnection();
                server.invokeWithCallback(serverConn, b1, new InvokeCallback() {
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
                        rets.add(e.getClass().getName());
                        latch.countDown();
                    }

                    @Override
                    public Executor getExecutor() {
                        return executor;
                    }

                }, timeout);
            }

        } catch (RemotingException e) {
            logger.error("Other RemotingException but RpcServerTimeoutException occurred in sync", e);
            Assert.fail("Should not reach here!");
        } catch (InterruptedException e) {
            logger.error("InterruptedException but RpcServerTimeoutException occurred in sync", e);
            Assert.fail("Should not reach here!");
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            String errMsg = "InterruptedException caught in callback!";
            logger.error(errMsg, e);
            Assert.fail(errMsg);
        }
        Assert.assertEquals(InvokeTimeoutException.class.getName(), rets.get(0));
        rets.clear();
    }

}
