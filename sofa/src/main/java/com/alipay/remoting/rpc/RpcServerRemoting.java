package com.alipay.remoting.rpc;

import com.alipay.remoting.*;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.util.RemotingUtil;

public class RpcServerRemoting extends RpcRemoting {

    public RpcServerRemoting(CommandFactory commandFactory) {
        super(commandFactory);
    }

    public RpcServerRemoting(CommandFactory commandFactory, RemotingAddressParser addressParser, DefaultConnectionManager connectionManager) {
        super(commandFactory, addressParser, connectionManager);
    }

    @Override
    public Object invokeSync(Url url, Object request, InvokeContext invokeContext, int timeoutMillis) throws RemotingException, InterruptedException {
        Connection conn = this.connectionManager.get(url.getUniqueKey());
        if (null == conn) {
            throw new RemotingException("Client address [" + url.getUniqueKey() + "] not connected yet!");
        }
        this.connectionManager.check(conn);
        return this.invokeSync(conn, request, invokeContext, timeoutMillis);
    }

    @Override
    public void oneway(Url url, Object request, InvokeContext invokeContext) throws RemotingException {
        Connection conn = this.connectionManager.get(url.getUniqueKey());
        if (null == conn) {
            throw new RemotingException("Client address [" + url.getOriginUrl() + "] not connected yet!");
        }
        this.connectionManager.check(conn);
        this.oneway(conn, request, invokeContext);
    }

    @Override
    public RpcResponseFuture invokeWithFuture(Url url, Object request, InvokeContext invokeContext, int timeoutMillis) throws RemotingException {
        Connection conn = this.connectionManager.get(url.getUniqueKey());
        if (null == conn) {
            throw new RemotingException("Client address [" + url.getUniqueKey() + "] not connected yet!");
        }
        this.connectionManager.check(conn);
        return this.invokeWithFuture(conn, request, invokeContext, timeoutMillis);
    }

    @Override
    public void invokeWithCallback(Url url, Object request, InvokeContext invokeContext, InvokeCallback invokeCallback, int timeoutMillis) throws RemotingException {
        Connection conn = this.connectionManager.get(url.getUniqueKey());
        if (null == conn) {
            throw new RemotingException("Client address [" + url.getUniqueKey() + "] not connected yet!");
        }
        this.connectionManager.check(conn);
        this.invokeWithCallback(conn, request, invokeContext, invokeCallback, timeoutMillis);
    }

    @Override
    protected void preProcessInvokeContext(InvokeContext invokeContext, RemotingCommand cmd, Connection connection) {
        if (null != invokeContext) {
            invokeContext.putIfAbsent(InvokeContext.SERVER_REMOTE_IP, RemotingUtil.parseRemoteIP(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.SERVER_REMOTE_PORT, RemotingUtil.parseRemotePort(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.SERVER_LOCAL_IP, RemotingUtil.parseLocalIP(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.SERVER_LOCAL_PORT, RemotingUtil.parseLocalPort(connection.getChannel()));
            invokeContext.putIfAbsent(InvokeContext.BOLT_INVOKE_REQUEST_ID, cmd.getId());
        }
    }

}
