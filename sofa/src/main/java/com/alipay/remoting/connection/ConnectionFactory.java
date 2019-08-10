package com.alipay.remoting.connection;

import com.alipay.remoting.Connection;
import com.alipay.remoting.ConnectionEventHandler;
import com.alipay.remoting.Url;

public interface ConnectionFactory {

    void init(ConnectionEventHandler connectionEventHandler);

    Connection createConnection(Url url) throws Exception;

    Connection createConnection(String targetIP, int targetPort, int connectTimeout) throws Exception;

    Connection createConnection(String targetIP, int targetPort, byte version, int connectTimeout) throws Exception;

}
