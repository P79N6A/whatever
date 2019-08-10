package com.alipay.remoting;

import com.alipay.remoting.config.Configurable;
import com.alipay.remoting.rpc.protocol.UserProcessor;

import java.util.concurrent.ExecutorService;

public interface RemotingServer extends Configurable, LifeCycle {

    @Deprecated
    void init();

    @Deprecated
    boolean start();

    @Deprecated
    boolean stop();

    String ip();

    int port();

    void registerProcessor(byte protocolCode, CommandCode commandCode, RemotingProcessor<?> processor);

    void registerDefaultExecutor(byte protocolCode, ExecutorService executor);

    void registerUserProcessor(UserProcessor<?> processor);

}
