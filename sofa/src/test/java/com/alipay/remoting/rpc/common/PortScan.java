package com.alipay.remoting.rpc.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class PortScan {
    private static final Logger logger = LoggerFactory.getLogger(PortScan.class);

    static public int select() {
        int port = -1;
        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.bind(null);
            port = ss.getLocalPort();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                ss.close();
                logger.warn("Server socket close status: {}", ss.isClosed());
            } catch (IOException e) {
            }
        }
        return port;
    }

    public static void main(String[] args) throws Exception {
        int port = PortScan.select();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(port));
        logger.warn("listening on port：{}", port);
        Thread.sleep(100);
        Socket s = new Socket("localhost", port);
        System.out.println(s.isConnected());
        System.out.println("local port: " + s.getLocalPort());
        System.out.println("remote port: " + s.getPort());
        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
    }

}
