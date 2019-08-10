package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;

import java.util.List;

public abstract class AsynMultiInterestUserProcessor<T> extends AbstractMultiInterestUserProcessor<T> {

    @Override
    public Object handleRequest(BizContext bizCtx, T request) throws Exception {
        throw new UnsupportedOperationException("SYNC handle request is unsupported in AsynMultiInterestUserProcessor!");
    }

    @Override
    public abstract void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request);

    @Override
    public abstract List<String> multiInterest();

}
