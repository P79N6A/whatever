package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;

public abstract class AsyncUserProcessor<T> extends AbstractUserProcessor<T> {

    @Override
    public Object handleRequest(BizContext bizCtx, T request) throws Exception {
        throw new UnsupportedOperationException("SYNC handle request is unsupported in AsyncUserProcessor!");
    }

    @Override
    public abstract void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request);

    @Override
    public abstract String interest();

}