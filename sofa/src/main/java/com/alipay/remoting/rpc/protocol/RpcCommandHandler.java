package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.*;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.*;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Sharable
public class RpcCommandHandler implements CommandHandler {

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    ProcessorManager processorManager;

    CommandFactory commandFactory;

    public RpcCommandHandler(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
        this.processorManager = new ProcessorManager();
        this.processorManager.registerProcessor(RpcCommandCode.RPC_REQUEST, new RpcRequestProcessor(this.commandFactory));
        this.processorManager.registerProcessor(RpcCommandCode.RPC_RESPONSE, new RpcResponseProcessor());
        this.processorManager.registerProcessor(CommonCommandCode.HEARTBEAT, new RpcHeartBeatProcessor());
        this.processorManager.registerDefaultProcessor(new AbstractRemotingProcessor<RemotingCommand>() {
            @Override
            public void doProcess(RemotingContext ctx, RemotingCommand msg) throws Exception {
                logger.error("No processor available for command code {}, msgId {}", msg.getCmdCode(), msg.getId());
            }
        });
    }

    @Override
    public void handleCommand(RemotingContext ctx, Object msg) throws Exception {
        this.handle(ctx, msg);
    }

    private void handle(final RemotingContext ctx, final Object msg) {
        try {
            // 批量
            if (msg instanceof List) {
                final Runnable handleTask = new Runnable() {
                    @Override
                    public void run() {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Batch message! size={}", ((List<?>) msg).size());
                        }
                        for (final Object m : (List<?>) msg) {
                            RpcCommandHandler.this.process(ctx, m);
                        }
                    }
                };
                // 交给线程池执行
                if (RpcConfigManager.dispatch_msg_list_in_default_executor()) {
                    processorManager.getDefaultExecutor().execute(handleTask);
                }
                // 本线程直接执行
                else {
                    handleTask.run();
                }
            }
            // 单个
            else {
                process(ctx, msg);
            }
        } catch (final Throwable t) {
            processException(ctx, msg, t);
        }
    }

    /**
     * 处理
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void process(RemotingContext ctx, Object msg) {
        try {
            final RpcCommand cmd = (RpcCommand) msg;
            // 处理器
            final RemotingProcessor processor = processorManager.getProcessor(cmd.getCmdCode());
            // 处理
            processor.process(ctx, cmd, processorManager.getDefaultExecutor());
        } catch (final Throwable t) {
            processException(ctx, msg, t);
        }
    }

    private void processException(RemotingContext ctx, Object msg, Throwable t) {
        if (msg instanceof List) {
            for (final Object m : (List<?>) msg) {
                processExceptionForSingleCommand(ctx, m, t);
            }
        } else {
            processExceptionForSingleCommand(ctx, msg, t);
        }
    }

    private void processExceptionForSingleCommand(RemotingContext ctx, Object msg, Throwable t) {
        final int id = ((RpcCommand) msg).getId();
        final String emsg = "Exception caught when processing " + ((msg instanceof RequestCommand) ? "request, id=" : "response, id=");
        logger.warn(emsg + id, t);
        if (msg instanceof RequestCommand) {
            final RequestCommand cmd = (RequestCommand) msg;
            if (cmd.getType() != RpcCommandType.REQUEST_ONEWAY) {
                if (t instanceof RejectedExecutionException) {
                    final ResponseCommand response = this.commandFactory.createExceptionResponse(id, ResponseStatus.SERVER_THREADPOOL_BUSY);
                    ctx.getChannelContext().writeAndFlush(response).addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("Write back exception response done, requestId={}, status={}", id, response.getResponseStatus());
                                }
                            } else {
                                logger.error("Write back exception response failed, requestId={}", id, future.cause());
                            }
                        }

                    });
                }
            }
        }
    }

    @Override
    public void registerProcessor(CommandCode cmd, @SuppressWarnings("rawtypes") RemotingProcessor processor) {
        this.processorManager.registerProcessor(cmd, processor);
    }

    @Override
    public void registerDefaultExecutor(ExecutorService executor) {
        this.processorManager.registerDefaultExecutor(executor);
    }

    @Override
    public ExecutorService getDefaultExecutor() {
        return this.processorManager.getDefaultExecutor();
    }

}
