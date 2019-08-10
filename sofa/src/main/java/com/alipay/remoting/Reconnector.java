package com.alipay.remoting;

public interface Reconnector extends LifeCycle {

    void reconnect(Url url);

    void disableReconnect(Url url);

    void enableReconnect(Url url);

}
