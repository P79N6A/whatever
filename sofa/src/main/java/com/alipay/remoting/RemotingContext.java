package com.alipay.remoting;

import com.alipay.remoting.rpc.RpcCommandType;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import com.alipay.remoting.util.ConnectionUtil;
import com.alipay.remoting.util.StringUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

public class RemotingContext {

    private ChannelHandlerContext channelContext;

    private boolean serverSide = false;

    private boolean timeoutDiscard = true;

    private long arriveTimestamp;

    private int timeout;

    private int rpcCommandType;

    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors;

    private InvokeContext invokeContext;

    public RemotingContext(ChannelHandlerContext ctx) {
        this.channelContext = ctx;
    }

    public RemotingContext(ChannelHandlerContext ctx, boolean serverSide) {
        this.channelContext = ctx;
        this.serverSide = serverSide;
    }

    public RemotingContext(ChannelHandlerContext ctx, boolean serverSide, ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this.channelContext = ctx;
        this.serverSide = serverSide;
        this.userProcessors = userProcessors;
    }

    public RemotingContext(ChannelHandlerContext ctx, InvokeContext invokeContext, boolean serverSide, ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this.channelContext = ctx;
        this.serverSide = serverSide;
        this.userProcessors = userProcessors;
        this.invokeContext = invokeContext;
    }

    public ChannelFuture writeAndFlush(RemotingCommand msg) {
        return this.channelContext.writeAndFlush(msg);
    }

    public boolean isRequestTimeout() {
        if (this.timeout > 0 && (this.rpcCommandType != RpcCommandType.REQUEST_ONEWAY) && (System.currentTimeMillis() - this.arriveTimestamp) > this.timeout) {
            return true;
        }
        return false;
    }

    public boolean isServerSide() {
        return this.serverSide;
    }

    public UserProcessor<?> getUserProcessor(String className) {
        return StringUtils.isBlank(className) ? null : this.userProcessors.get(className);
    }

    public Connection getConnection() {
        return ConnectionUtil.getConnectionFromChannel(channelContext.channel());
    }

    public ChannelHandlerContext getChannelContext() {
        return channelContext;
    }

    public void setChannelContext(ChannelHandlerContext ctx) {
        this.channelContext = ctx;
    }

    public InvokeContext getInvokeContext() {
        return invokeContext;
    }

    public void setArriveTimestamp(long arriveTimestamp) {
        this.arriveTimestamp = arriveTimestamp;
    }

    public long getArriveTimestamp() {
        return arriveTimestamp;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setRpcCommandType(int rpcCommandType) {
        this.rpcCommandType = rpcCommandType;
    }

    public boolean isTimeoutDiscard() {
        return timeoutDiscard;
    }

    public RemotingContext setTimeoutDiscard(boolean failFastEnabled) {
        this.timeoutDiscard = failFastEnabled;
        return this;
    }

}
