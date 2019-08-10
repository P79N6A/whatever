package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.RemotingContext;

import java.util.concurrent.Executor;

public interface UserProcessor<T> {

    BizContext preHandleRequest(RemotingContext remotingCtx, T request);

    void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request);

    Object handleRequest(BizContext bizCtx, T request) throws Exception;

    String interest();

    Executor getExecutor();

    boolean processInIOThread();

    boolean timeoutDiscard();

    void setExecutorSelector(ExecutorSelector executorSelector);

    ExecutorSelector getExecutorSelector();

    interface ExecutorSelector {
        Executor select(String requestClass, Object requestHeader);

    }

}
