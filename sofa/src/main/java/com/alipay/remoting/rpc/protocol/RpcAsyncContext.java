package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.RemotingContext;

import java.util.concurrent.atomic.AtomicBoolean;

public class RpcAsyncContext implements AsyncContext {

    private RemotingContext ctx;

    private RpcRequestCommand cmd;

    private RpcRequestProcessor processor;

    private AtomicBoolean isResponseSentAlready = new AtomicBoolean();

    public RpcAsyncContext(final RemotingContext ctx, final RpcRequestCommand cmd, final RpcRequestProcessor processor) {
        this.ctx = ctx;
        this.cmd = cmd;
        this.processor = processor;
    }

    @Override
    public void sendResponse(Object responseObject) {
        if (isResponseSentAlready.compareAndSet(false, true)) {
            processor.sendResponseIfNecessary(this.ctx, cmd.getType(), processor.getCommandFactory().createResponse(responseObject, this.cmd));
        } else {
            throw new IllegalStateException("Should not send rpc response repeatedly!");
        }
    }

}