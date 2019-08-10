package com.alipay.remoting;

import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.log.BoltLoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.*;

public class ProcessorManager {
    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private ConcurrentHashMap<CommandCode, RemotingProcessor<?>> cmd2processors = new ConcurrentHashMap<CommandCode, RemotingProcessor<?>>(4);

    private RemotingProcessor<?> defaultProcessor;

    private ExecutorService defaultExecutor;

    private int minPoolSize = ConfigManager.default_tp_min_size();

    private int maxPoolSize = ConfigManager.default_tp_max_size();

    private int queueSize = ConfigManager.default_tp_queue_size();

    private long keepAliveTime = ConfigManager.default_tp_keepalive_time();

    public ProcessorManager() {
        defaultExecutor = new ThreadPoolExecutor(minPoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize), new NamedThreadFactory("Bolt-default-executor", true));
    }

    public void registerProcessor(CommandCode cmdCode, RemotingProcessor<?> processor) {
        if (this.cmd2processors.containsKey(cmdCode)) {
            logger.warn("Processor for cmd={} is already registered, the processor is {}, and changed to {}", cmdCode, cmd2processors.get(cmdCode).getClass().getName(), processor.getClass().getName());
        }
        this.cmd2processors.put(cmdCode, processor);
    }

    public void registerDefaultProcessor(RemotingProcessor<?> processor) {
        if (this.defaultProcessor == null) {
            this.defaultProcessor = processor;
        } else {
            throw new IllegalStateException("The defaultProcessor has already been registered: " + this.defaultProcessor.getClass());
        }
    }

    public RemotingProcessor<?> getProcessor(CommandCode cmdCode) {
        RemotingProcessor<?> processor = this.cmd2processors.get(cmdCode);
        if (processor != null) {
            return processor;
        }
        return this.defaultProcessor;
    }

    public ExecutorService getDefaultExecutor() {
        return defaultExecutor;
    }

    public void registerDefaultExecutor(ExecutorService executor) {
        this.defaultExecutor = executor;
    }

}