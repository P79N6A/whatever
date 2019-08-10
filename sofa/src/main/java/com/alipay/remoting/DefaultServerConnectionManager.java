package com.alipay.remoting;

public class DefaultServerConnectionManager extends DefaultConnectionManager implements ServerConnectionManager {

    public DefaultServerConnectionManager(ConnectionSelectStrategy connectionSelectStrategy) {
        super(connectionSelectStrategy);
    }

}
