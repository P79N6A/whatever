package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.DefaultBizContext;
import com.alipay.remoting.RemotingContext;

import java.util.concurrent.Executor;

public abstract class AbstractUserProcessor<T> implements UserProcessor<T> {

    protected ExecutorSelector executorSelector;

    @Override
    public BizContext preHandleRequest(RemotingContext remotingCtx, T request) {
        return new DefaultBizContext(remotingCtx);
    }

    @Override
    public Executor getExecutor() {
        return null;
    }

    @Override
    public ExecutorSelector getExecutorSelector() {
        return this.executorSelector;
    }

    @Override
    public void setExecutorSelector(ExecutorSelector executorSelector) {
        this.executorSelector = executorSelector;
    }

    @Override
    public boolean processInIOThread() {
        return false;
    }

    @Override
    public boolean timeoutDiscard() {
        return true;
    }

}