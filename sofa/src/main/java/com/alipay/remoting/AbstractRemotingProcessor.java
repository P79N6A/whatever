package com.alipay.remoting;

import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.RemotingUtil;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

public abstract class AbstractRemotingProcessor<T extends RemotingCommand> implements RemotingProcessor<T> {
    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private ExecutorService executor;

    private CommandFactory commandFactory;

    public AbstractRemotingProcessor() {
    }

    public AbstractRemotingProcessor(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public AbstractRemotingProcessor(ExecutorService executor) {
        this.executor = executor;
    }

    public AbstractRemotingProcessor(CommandFactory commandFactory, ExecutorService executor) {
        this.commandFactory = commandFactory;
        this.executor = executor;
    }

    public abstract void doProcess(RemotingContext ctx, T msg) throws Exception;

    @Override
    public void process(RemotingContext ctx, T msg, ExecutorService defaultExecutor) throws Exception {
        ProcessTask task = new ProcessTask(ctx, msg);
        // 指定线程池
        if (this.getExecutor() != null) {
            this.getExecutor().execute(task);
        }
        // 默认线程池
        else {
            defaultExecutor.execute(task);
        }
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public CommandFactory getCommandFactory() {
        return commandFactory;
    }

    public void setCommandFactory(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    class ProcessTask implements Runnable {

        RemotingContext ctx;

        T msg;

        public ProcessTask(RemotingContext ctx, T msg) {
            this.ctx = ctx;
            this.msg = msg;
        }

        @Override
        public void run() {
            try {
                AbstractRemotingProcessor.this.doProcess(ctx, msg);
            } catch (Throwable e) {
                String remotingAddress = RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel());
                logger.error("Exception caught when process rpc request command in AbstractRemotingProcessor, Id=" + msg.getId() + "! Invoke source address is [" + remotingAddress + "].", e);
            }
        }

    }

}
