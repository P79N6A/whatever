package com.alipay.remoting;

import com.alipay.remoting.exception.RemotingException;

import java.util.List;
import java.util.Map;

public interface ConnectionManager extends Scannable, LifeCycle {

    @Deprecated
    void init();

    void add(Connection connection);

    void add(Connection connection, String poolKey);

    Connection get(String poolKey);

    List<Connection> getAll(String poolKey);

    Map<String, List<Connection>> getAll();

    void remove(Connection connection);

    void remove(Connection connection, String poolKey);

    void remove(String poolKey);

    @Deprecated
    void removeAll();

    void check(Connection connection) throws RemotingException;

    int count(String poolKey);

    Connection getAndCreateIfAbsent(Url url) throws InterruptedException, RemotingException;

    void createConnectionAndHealIfNeed(Url url) throws InterruptedException, RemotingException;

    Connection create(Url url) throws RemotingException;

    Connection create(String address, int connectTimeout) throws RemotingException;

    Connection create(String ip, int port, int connectTimeout) throws RemotingException;

}
