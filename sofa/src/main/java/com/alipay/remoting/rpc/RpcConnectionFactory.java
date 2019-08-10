package com.alipay.remoting.rpc;

import com.alipay.remoting.config.ConfigurableInstance;
import com.alipay.remoting.connection.DefaultConnectionFactory;
import com.alipay.remoting.rpc.protocol.UserProcessor;

import java.util.concurrent.ConcurrentHashMap;

public class RpcConnectionFactory extends DefaultConnectionFactory {

    public RpcConnectionFactory(ConcurrentHashMap<String, UserProcessor<?>> userProcessors, ConfigurableInstance configInstance) {
        super(new RpcCodec(), new HeartbeatHandler(), new RpcHandler(userProcessors), configInstance);
    }

}