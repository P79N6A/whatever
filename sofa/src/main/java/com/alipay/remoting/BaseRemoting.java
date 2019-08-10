package com.alipay.remoting;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.RemotingUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public abstract class BaseRemoting {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    protected CommandFactory commandFactory;

    public BaseRemoting(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    protected RemotingCommand invokeSync(final Connection conn, final RemotingCommand request, final int timeoutMillis) throws RemotingException, InterruptedException {
        // 根据requestId创建Future
        final InvokeFuture future = createInvokeFuture(request, request.getInvokeContext());
        // 待回复
        conn.addInvokeFuture(future);
        // 获取requestId
        final int requestId = request.getId();
        try {
            conn.getChannel().writeAndFlush(request).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    // 发送失败
                    if (!f.isSuccess()) {
                        // 移除
                        conn.removeInvokeFuture(requestId);
                        // 唤醒线程
                        future.putResponse(commandFactory.createSendFailedResponse(conn.getRemoteAddress(), f.cause()));
                        logger.error("Invoke send failed, id={}", requestId, f.cause());
                    }
                }

            });
        } catch (Exception e) {
            // 发生异常
            conn.removeInvokeFuture(requestId);
            future.putResponse(commandFactory.createSendFailedResponse(conn.getRemoteAddress(), e));
            logger.error("Exception caught when sending invocation, id={}", requestId, e);
        }
        // 阻塞等待
        RemotingCommand response = future.waitResponse(timeoutMillis);
        if (response == null) {
            conn.removeInvokeFuture(requestId);
            // 超时
            response = this.commandFactory.createTimeoutResponse(conn.getRemoteAddress());
            logger.warn("Wait response, request id={} timeout!", requestId);
        }
        return response;
    }

    protected void invokeWithCallback(final Connection conn, final RemotingCommand request, final InvokeCallback invokeCallback, final int timeoutMillis) {
        final InvokeFuture future = createInvokeFuture(conn, request, request.getInvokeContext(), invokeCallback);
        conn.addInvokeFuture(future);
        final int requestId = request.getId();
        try {
            Timeout timeout = TimerHolder.getTimer().newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    InvokeFuture future = conn.removeInvokeFuture(requestId);
                    if (future != null) {
                        future.putResponse(commandFactory.createTimeoutResponse(conn.getRemoteAddress()));
                        future.tryAsyncExecuteInvokeCallbackAbnormally();
                    }
                }

            }, timeoutMillis, TimeUnit.MILLISECONDS);
            future.addTimeout(timeout);
            conn.getChannel().writeAndFlush(request).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture cf) throws Exception {
                    if (!cf.isSuccess()) {
                        InvokeFuture f = conn.removeInvokeFuture(requestId);
                        if (f != null) {
                            f.cancelTimeout();
                            f.putResponse(commandFactory.createSendFailedResponse(conn.getRemoteAddress(), cf.cause()));
                            f.tryAsyncExecuteInvokeCallbackAbnormally();
                        }
                        logger.error("Invoke send failed. The address is {}", RemotingUtil.parseRemoteAddress(conn.getChannel()), cf.cause());
                    }
                }

            });
        } catch (Exception e) {
            InvokeFuture f = conn.removeInvokeFuture(requestId);
            if (f != null) {
                f.cancelTimeout();
                f.putResponse(commandFactory.createSendFailedResponse(conn.getRemoteAddress(), e));
                f.tryAsyncExecuteInvokeCallbackAbnormally();
            }
            logger.error("Exception caught when sending invocation. The address is {}", RemotingUtil.parseRemoteAddress(conn.getChannel()), e);
        }
    }

    protected InvokeFuture invokeWithFuture(final Connection conn, final RemotingCommand request, final int timeoutMillis) {
        final InvokeFuture future = createInvokeFuture(request, request.getInvokeContext());
        conn.addInvokeFuture(future);
        final int requestId = request.getId();
        try {
            Timeout timeout = TimerHolder.getTimer().newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    InvokeFuture future = conn.removeInvokeFuture(requestId);
                    if (future != null) {
                        future.putResponse(commandFactory.createTimeoutResponse(conn.getRemoteAddress()));
                    }
                }

            }, timeoutMillis, TimeUnit.MILLISECONDS);
            future.addTimeout(timeout);
            conn.getChannel().writeAndFlush(request).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture cf) throws Exception {
                    if (!cf.isSuccess()) {
                        InvokeFuture f = conn.removeInvokeFuture(requestId);
                        if (f != null) {
                            f.cancelTimeout();
                            f.putResponse(commandFactory.createSendFailedResponse(conn.getRemoteAddress(), cf.cause()));
                        }
                        logger.error("Invoke send failed. The address is {}", RemotingUtil.parseRemoteAddress(conn.getChannel()), cf.cause());
                    }
                }

            });
        } catch (Exception e) {
            InvokeFuture f = conn.removeInvokeFuture(requestId);
            if (f != null) {
                f.cancelTimeout();
                f.putResponse(commandFactory.createSendFailedResponse(conn.getRemoteAddress(), e));
            }
            logger.error("Exception caught when sending invocation. The address is {}", RemotingUtil.parseRemoteAddress(conn.getChannel()), e);
        }
        return future;
    }

    protected void oneway(final Connection conn, final RemotingCommand request) {
        try {
            conn.getChannel().writeAndFlush(request).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (!f.isSuccess()) {
                        logger.error("Invoke send failed. The address is {}", RemotingUtil.parseRemoteAddress(conn.getChannel()), f.cause());
                    }
                }

            });
        } catch (Exception e) {
            if (null == conn) {
                logger.error("Conn is null");
            } else {
                logger.error("Exception caught when sending invocation. The address is {}", RemotingUtil.parseRemoteAddress(conn.getChannel()), e);
            }
        }
    }

    protected abstract InvokeFuture createInvokeFuture(final RemotingCommand request, final InvokeContext invokeContext);

    protected abstract InvokeFuture createInvokeFuture(final Connection conn, final RemotingCommand request, final InvokeContext invokeContext, final InvokeCallback invokeCallback);

    protected CommandFactory getCommandFactory() {
        return commandFactory;
    }

}
