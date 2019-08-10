package com.alipay.remoting;

import java.util.concurrent.ExecutorService;

public interface RemotingProcessor<T extends RemotingCommand> {

    void process(RemotingContext ctx, T msg, ExecutorService defaultExecutor) throws Exception;

    ExecutorService getExecutor();

    void setExecutor(ExecutorService executor);

}
