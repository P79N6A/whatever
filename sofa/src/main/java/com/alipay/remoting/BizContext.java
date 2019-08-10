package com.alipay.remoting;

public interface BizContext {

    String getRemoteAddress();

    String getRemoteHost();

    int getRemotePort();

    Connection getConnection();

    boolean isRequestTimeout();

    int getClientTimeout();

    long getArriveTimestamp();

    void put(String key, String value);

    String get(String key);

    InvokeContext getInvokeContext();

}