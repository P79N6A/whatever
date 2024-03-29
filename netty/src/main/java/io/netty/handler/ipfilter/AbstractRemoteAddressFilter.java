package io.netty.handler.ipfilter;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.SocketAddress;

public abstract class AbstractRemoteAddressFilter<T extends SocketAddress> extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        handleNewChannel(ctx);
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!handleNewChannel(ctx)) {
            throw new IllegalStateException("cannot determine to accept or reject a channel: " + ctx.channel());
        } else {
            ctx.fireChannelActive();
        }
    }

    private boolean handleNewChannel(ChannelHandlerContext ctx) throws Exception {
        @SuppressWarnings("unchecked") T remoteAddress = (T) ctx.channel().remoteAddress();

        if (remoteAddress == null) {
            return false;
        }

        ctx.pipeline().remove(this);

        if (accept(ctx, remoteAddress)) {
            channelAccepted(ctx, remoteAddress);
        } else {
            ChannelFuture rejectedFuture = channelRejected(ctx, remoteAddress);
            if (rejectedFuture != null) {
                rejectedFuture.addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.close();
            }
        }

        return true;
    }

    protected abstract boolean accept(ChannelHandlerContext ctx, T remoteAddress) throws Exception;

    @SuppressWarnings("UnusedParameters")
    protected void channelAccepted(ChannelHandlerContext ctx, T remoteAddress) {
    }

    @SuppressWarnings("UnusedParameters")
    protected ChannelFuture channelRejected(ChannelHandlerContext ctx, T remoteAddress) {
        return null;
    }
}
