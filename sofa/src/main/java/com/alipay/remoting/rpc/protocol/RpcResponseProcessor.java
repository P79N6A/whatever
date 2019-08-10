package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.*;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.RemotingUtil;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

public class RpcResponseProcessor extends AbstractRemotingProcessor<RemotingCommand> {

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    public RpcResponseProcessor() {
    }

    public RpcResponseProcessor(ExecutorService executor) {
        super(executor);
    }

    @Override
    public void doProcess(RemotingContext ctx, RemotingCommand cmd) {
        Connection conn = ctx.getChannelContext().channel().attr(Connection.CONNECTION).get();
        InvokeFuture future = conn.removeInvokeFuture(cmd.getId());
        ClassLoader oldClassLoader = null;
        try {
            if (future != null) {
                if (future.getAppClassLoader() != null) {
                    oldClassLoader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(future.getAppClassLoader());
                }
                future.putResponse(cmd);
                future.cancelTimeout();
                try {
                    future.executeInvokeCallback();
                } catch (Exception e) {
                    logger.error("Exception caught when executing invoke callback, id={}", cmd.getId(), e);
                }
            } else {
                logger.warn("Cannot find InvokeFuture, maybe already timeout, id={}, from={} ", cmd.getId(), RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel()));
            }
        } finally {
            if (null != oldClassLoader) {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }

    }

}
