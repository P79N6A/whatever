package com.alipay.remoting;

public interface ConnectionEventProcessor {

    void onEvent(String remoteAddress, Connection connection);

}
