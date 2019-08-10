package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.*;
import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.DefaultInvokeFuture;
import com.alipay.remoting.rpc.HeartbeatCommand;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.util.RemotingUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public class RpcHeartbeatTrigger implements HeartbeatTrigger {
    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    public static final Integer maxCount = ConfigManager.tcp_idle_maxtimes();

    private static final long heartbeatTimeoutMillis = 1000;

    private CommandFactory commandFactory;

    public RpcHeartbeatTrigger(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @Override
    public void heartbeatTriggered(final ChannelHandlerContext ctx) throws Exception {
        Integer heartbeatTimes = ctx.channel().attr(Connection.HEARTBEAT_COUNT).get();
        final Connection conn = ctx.channel().attr(Connection.CONNECTION).get();
        // 心跳失败次数过多
        if (heartbeatTimes >= maxCount) {
            try {
                conn.close();
                logger.error("Heartbeat failed for {} times, close the connection from client side: {} ", heartbeatTimes, RemotingUtil.parseRemoteAddress(ctx.channel()));
            } catch (Exception e) {
                logger.warn("Exception caught when closing connection in SharableHandler.", e);
            }
        } else {
            boolean heartbeatSwitch = ctx.channel().attr(Connection.HEARTBEAT_SWITCH).get();
            if (!heartbeatSwitch) {
                return;
            }
            // 心跳包
            final HeartbeatCommand heartbeat = new HeartbeatCommand();
            // 心跳Future
            final InvokeFuture future = new DefaultInvokeFuture(heartbeat.getId(), new InvokeCallbackListener() {
                // 收到心跳回复
                @Override
                public void onResponse(InvokeFuture future) {
                    ResponseCommand response;
                    try {
                        response = (ResponseCommand) future.waitResponse(0);
                    } catch (InterruptedException e) {
                        logger.error("Heartbeat ack process error! Id={}, from remoteAddr={}", heartbeat.getId(), RemotingUtil.parseRemoteAddress(ctx.channel()), e);
                        return;
                    }
                    if (response != null && response.getResponseStatus() == ResponseStatus.SUCCESS) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Heartbeat ack received! Id={}, from remoteAddr={}", response.getId(), RemotingUtil.parseRemoteAddress(ctx.channel()));
                        }
                        // 心跳失败次数置零
                        ctx.channel().attr(Connection.HEARTBEAT_COUNT).set(0);
                    } else {
                        if (response != null && response.getResponseStatus() == ResponseStatus.TIMEOUT) {
                            logger.error("Heartbeat timeout! The address is {}", RemotingUtil.parseRemoteAddress(ctx.channel()));
                        } else {
                            logger.error("Heartbeat exception caught! Error code={}, The address is {}", response == null ? null : response.getResponseStatus(), RemotingUtil.parseRemoteAddress(ctx.channel()));
                        }
                        Integer times = ctx.channel().attr(Connection.HEARTBEAT_COUNT).get();
                        // 心跳失败次数++
                        ctx.channel().attr(Connection.HEARTBEAT_COUNT).set(times + 1);
                    }
                }

                @Override
                public String getRemoteAddress() {
                    return ctx.channel().remoteAddress().toString();
                }
            }, null, heartbeat.getProtocolCode().getFirstByte(), this.commandFactory);
            final int heartbeatId = heartbeat.getId();
            // 加入待处理
            conn.addInvokeFuture(future);
            if (logger.isDebugEnabled()) {
                logger.debug("Send heartbeat, successive count={}, Id={}, to remoteAddr={}", heartbeatTimes, heartbeatId, RemotingUtil.parseRemoteAddress(ctx.channel()));
            }
            // 发送心跳
            ctx.writeAndFlush(heartbeat).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Send heartbeat done! Id={}, to remoteAddr={}", heartbeatId, RemotingUtil.parseRemoteAddress(ctx.channel()));
                        }
                    } else {
                        logger.error("Send heartbeat failed! Id={}, to remoteAddr={}", heartbeatId, RemotingUtil.parseRemoteAddress(ctx.channel()));
                    }
                }
            });
            // 心跳超时无应答
            TimerHolder.getTimer().newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    InvokeFuture future = conn.removeInvokeFuture(heartbeatId);
                    if (future != null) {
                        future.putResponse(commandFactory.createTimeoutResponse(conn.getRemoteAddress()));
                        future.tryAsyncExecuteInvokeCallbackAbnormally();
                    }
                }
            }, heartbeatTimeoutMillis, TimeUnit.MILLISECONDS);
        }

    }

}
