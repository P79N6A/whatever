package com.alipay.remoting;

import com.alipay.remoting.config.Configurable;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcResponseFuture;
import com.alipay.remoting.rpc.protocol.UserProcessor;

import java.util.List;
import java.util.Map;

public interface BoltClient extends Configurable, LifeCycle {

    void oneway(final String address, final Object request) throws RemotingException, InterruptedException;

    void oneway(final String address, final Object request, final InvokeContext invokeContext) throws RemotingException, InterruptedException;

    void oneway(final Url url, final Object request) throws RemotingException, InterruptedException;

    void oneway(final Url url, final Object request, final InvokeContext invokeContext) throws RemotingException, InterruptedException;

    void oneway(final Connection conn, final Object request) throws RemotingException;

    void oneway(final Connection conn, final Object request, final InvokeContext invokeContext) throws RemotingException;

    Object invokeSync(final String address, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException;

    Object invokeSync(final String address, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException;

    Object invokeSync(final Url url, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException;

    Object invokeSync(final Url url, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException;

    Object invokeSync(final Connection conn, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException;

    Object invokeSync(final Connection conn, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException;

    RpcResponseFuture invokeWithFuture(final String address, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException;

    RpcResponseFuture invokeWithFuture(final String address, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException;

    RpcResponseFuture invokeWithFuture(final Url url, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException;

    RpcResponseFuture invokeWithFuture(final Url url, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException;

    RpcResponseFuture invokeWithFuture(final Connection conn, final Object request, int timeoutMillis) throws RemotingException;

    RpcResponseFuture invokeWithFuture(final Connection conn, final Object request, final InvokeContext invokeContext, int timeoutMillis) throws RemotingException;

    void invokeWithCallback(final String addr, final Object request, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException;

    void invokeWithCallback(final String addr, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException;

    void invokeWithCallback(final Url url, final Object request, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException;

    void invokeWithCallback(final Url url, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException;

    void invokeWithCallback(final Connection conn, final Object request, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException;

    void invokeWithCallback(final Connection conn, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException;

    void addConnectionEventProcessor(ConnectionEventType type, ConnectionEventProcessor processor);

    void registerUserProcessor(UserProcessor<?> processor);

    Connection createStandaloneConnection(String ip, int port, int connectTimeout) throws RemotingException;

    Connection createStandaloneConnection(String address, int connectTimeout) throws RemotingException;

    void closeStandaloneConnection(Connection conn);

    Connection getConnection(String addr, int connectTimeout) throws RemotingException, InterruptedException;

    Connection getConnection(Url url, int connectTimeout) throws RemotingException, InterruptedException;

    Map<String, List<Connection>> getAllManagedConnections();

    boolean checkConnection(String address);

    void closeConnection(String address);

    void closeConnection(Url url);

    void enableConnHeartbeat(String address);

    void enableConnHeartbeat(Url url);

    void disableConnHeartbeat(String address);

    void disableConnHeartbeat(Url url);

    void enableReconnectSwitch();

    void disableReconnectSwith();

    boolean isReconnectSwitchOn();

    void enableConnectionMonitorSwitch();

    void disableConnectionMonitorSwitch();

    boolean isConnectionMonitorSwitchOn();

    DefaultConnectionManager getConnectionManager();

    RemotingAddressParser getAddressParser();

    void setAddressParser(RemotingAddressParser addressParser);

    void setMonitorStrategy(ConnectionMonitorStrategy monitorStrategy);

}
