package com.alipay.remoting.rpc;

import com.alipay.remoting.*;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class RpcHandler extends ChannelInboundHandlerAdapter {
    private boolean serverSide;

    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors;

    public RpcHandler() {
        serverSide = false;
    }

    public RpcHandler(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        serverSide = false;
        this.userProcessors = userProcessors;
    }

    public RpcHandler(boolean serverSide, ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this.serverSide = serverSide;
        this.userProcessors = userProcessors;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 协议号
        ProtocolCode protocolCode = ctx.channel().attr(Connection.PROTOCOL).get();
        // 协议
        Protocol protocol = ProtocolManager.getProtocol(protocolCode);
        // 处理协议
        protocol.getCommandHandler().handleCommand(new RemotingContext(ctx, new InvokeContext(), serverSide, userProcessors), msg);
    }

}
