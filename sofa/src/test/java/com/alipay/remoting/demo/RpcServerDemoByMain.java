package com.alipay.remoting.demo;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.rpc.common.BoltServer;
import com.alipay.remoting.rpc.common.CONNECTEventProcessor;
import com.alipay.remoting.rpc.common.DISCONNECTEventProcessor;
import com.alipay.remoting.rpc.common.SimpleServerUserProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServerDemoByMain {
    static Logger logger = LoggerFactory.getLogger(RpcServerDemoByMain.class);

    BoltServer server;

    int port = 8999;

    SimpleServerUserProcessor serverUserProcessor = new SimpleServerUserProcessor();

    CONNECTEventProcessor serverConnectProcessor = new CONNECTEventProcessor();

    DISCONNECTEventProcessor serverDisConnectProcessor = new DISCONNECTEventProcessor();

    public RpcServerDemoByMain() {
        server = new BoltServer(port);
        server.addConnectionEventProcessor(ConnectionEventType.CONNECT, serverConnectProcessor);
        server.addConnectionEventProcessor(ConnectionEventType.CLOSE, serverDisConnectProcessor);
        server.registerUserProcessor(serverUserProcessor);
        if (server.start()) {
            System.out.println("server start ok!");
        } else {
            System.out.println("server start failed!");
        }

    }

    public static void main(String[] args) {
        new RpcServerDemoByMain();
    }

}
