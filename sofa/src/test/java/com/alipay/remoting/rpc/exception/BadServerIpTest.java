package com.alipay.remoting.rpc.exception;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.common.RequestBody;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public class BadServerIpTest {

    Logger logger = LoggerFactory.getLogger(BadServerIpTest.class);

    @Test
    public void cantAssignTest() {
        BadServer server = new BadServer("59.66.132.166");
        try {
            server.startServer();
        } catch (Exception e) {
            logger.error("Start server failed!", e);
        }
    }

    @Test
    public void cantResolveTest() {
        BadServer server = new BadServer("59.66.132.1666");
        try {
            server.startServer();
        } catch (Exception e) {
            logger.error("Start server failed!", e);
        }
    }

    class BadServer {

        Logger logger = LoggerFactory.getLogger(BadServer.class);

        RpcServer server;

        String ip;

        public BadServer(String ip) {
            this.ip = ip;
        }

        public void startServer() {
            server = new RpcServer(ip, 1111);
            server.registerUserProcessor(new SyncUserProcessor<RequestBody>() {
                @Override
                public Object handleRequest(BizContext bizCtx, RequestBody request) throws Exception {
                    logger.warn("Request received:" + request);
                    return "hello world!";
                }

                @Override
                public String interest() {
                    return String.class.getName();
                }

                @Override
                public Executor getExecutor() {
                    return null;
                }

            });
            server.start();
        }

    }

}