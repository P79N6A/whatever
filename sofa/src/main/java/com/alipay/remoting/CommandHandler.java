package com.alipay.remoting;

import java.util.concurrent.ExecutorService;

public interface CommandHandler {

    void handleCommand(RemotingContext ctx, Object msg) throws Exception;

    void registerProcessor(CommandCode cmd, RemotingProcessor<?> processor);

    void registerDefaultExecutor(ExecutorService executor);

    ExecutorService getDefaultExecutor();

}
