package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;

import java.util.List;

public abstract class SyncMutiInterestUserProcessor<T> extends AbstractMultiInterestUserProcessor<T> {

    @Override
    public abstract Object handleRequest(BizContext bizCtx, T request) throws Exception;

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) {
        throw new UnsupportedOperationException("ASYNC handle request is unsupported in SyncMutiInterestUserProcessor!");
    }

    @Override
    public abstract List<String> multiInterest();

}
