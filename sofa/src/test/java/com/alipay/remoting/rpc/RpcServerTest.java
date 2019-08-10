package com.alipay.remoting.rpc;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServerTest {
    static Logger logger = LoggerFactory.getLogger(RpcServerTest.class);

    @Before
    public void init() {
    }

    @After
    public void stop() {
    }

    @Test
    public void doTestStartAndStop() {
        doTestStartAndStop(true);
        doTestStartAndStop(false);
    }

    private void doTestStartAndStop(boolean syncStop) {
        RpcServer rpcServer1 = new RpcServer(1111, false, syncStop);
        try {
            rpcServer1.start();
        } catch (Exception e) {
            logger.warn("start fail");
            Assert.fail("Should not reach here");
        }
        logger.warn("start success");
        RpcServer rpcServer2 = new RpcServer(1111, false, syncStop);
        try {
            rpcServer2.start();
            Assert.fail("Should not reach here");
            logger.warn("start success");
        } catch (Exception e) {
            logger.warn("start fail");
        }
        try {
            rpcServer1.stop();
        } catch (IllegalStateException e) {
            Assert.fail("Should not reach here");
        }
        try {
            rpcServer2.stop();
            Assert.fail("Should not reach here");
        } catch (Exception e) {
        }
    }

}