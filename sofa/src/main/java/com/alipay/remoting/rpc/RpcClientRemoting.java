package com.alipay.remoting.rpc;

import com.alipay.remoting.*;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.util.RemotingUtil;

public class RpcClientRemoting extends RpcRemoting {

    public RpcClientRemoting(CommandFactory commandFactory, RemotingAddressParser addressParser, DefaultConnectionManager connectionManager) {
        super(commandFactory, addressParser, connectionManager);
    }

    @Override
    public void oneway(Url url, Object request, InvokeContext invokeContext) throws RemotingException, InterruptedException {
        final Connection conn = getConnectionAndInitInvokeContext(url, invokeContext);
        this.connectionManager.check(conn);
        this.oneway(conn, request, invokeContext);
    }

    @Override
    public Object invokeSync(Url url, Object request, InvokeContext invokeContext, int timeoutMillis) throws RemotingException, InterruptedException {
        final Connection conn = getConnectionAndInitInvokeContext(url, invokeContext);
        this.connectionManager.check(conn);
        return this.invokeSync(conn, request, invokeContext, timeoutMillis);
    }

    @Override
    public RpcResponseFuture invokeWithFuture(Url url, Object request, InvokeContext invokeContext, int timeoutMillis) throws RemotingException, InterruptedException {
        final Connection conn = getConnectionAndInitInvokeContext(url, invokeContext);
        this.connectionManager.check(conn);
        return this.invokeWithFuture(conn, request, invokeContext, timeoutMillis);
    }

    @Override
    public void invokeWithCallback(Url url, Object request, InvokeContext invokeContext, InvokeCallback invokeCallback, int timeoutMillis) throws RemotingException, InterruptedException {
        final Connection conn = getConnectionAndInitInvokeContext(url, invokeContext);
        this.connectionManager.check(conn);
        this.invokeWithCallback(conn, request, invokeContext, invokeCallback, timeoutMillis);
    }

    @Override
    protected void preProcessInvokeContext(InvokeContext invokeContext, RemotingCommand cmd, Connection connection) {
        if (null != invokeContext) {
            invokeContext.putIfAbsent(InvokeContext.CLIENT_LOCAL_IP, RemotingUtil.parseLocalIP(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.CLIENT_LOCAL_PORT, RemotingUtil.parseLocalPort(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.CLIENT_REMOTE_IP, RemotingUtil.parseRemoteIP(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.CLIENT_REMOTE_PORT, RemotingUtil.parseRemotePort(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.BOLT_INVOKE_REQUEST_ID, cmd.getId());
        }
    }

    /**
     * 获取连接，懒加载（连接）
     */
    protected Connection getConnectionAndInitInvokeContext(Url url, InvokeContext invokeContext) throws RemotingException, InterruptedException {
        long start = System.currentTimeMillis();
        Connection conn;
        try {
            // 从连接管理获取
            conn = this.connectionManager.getAndCreateIfAbsent(url);
        } finally {
            if (null != invokeContext) {
                invokeContext.putIfAbsent(InvokeContext.CLIENT_CONN_CREATETIME, (System.currentTimeMillis() - start));
            }
        }
        return conn;
    }

}
