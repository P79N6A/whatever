package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.*;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.RpcCommandType;
import com.alipay.remoting.util.RemotingUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class RpcRequestProcessor extends AbstractRemotingProcessor<RpcRequestCommand> {

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    public RpcRequestProcessor() {
    }

    public RpcRequestProcessor(CommandFactory commandFactory) {
        super(commandFactory);
    }

    public RpcRequestProcessor(ExecutorService executor) {
        super(executor);
    }

    @Override
    public void process(RemotingContext ctx, RpcRequestCommand cmd, ExecutorService defaultExecutor) throws Exception {
        // 先反序列化Command
        if (!deserializeRequestCommand(ctx, cmd, RpcDeserializeLevel.DESERIALIZE_CLAZZ)) {
            return;
        }
        UserProcessor userProcessor = ctx.getUserProcessor(cmd.getRequestClass());
        if (userProcessor == null) {
            String errMsg = "No user processor found for request: " + cmd.getRequestClass();
            logger.error(errMsg);
            sendResponseIfNecessary(ctx, cmd.getType(), this.getCommandFactory().createExceptionResponse(cmd.getId(), errMsg));
            return;
        }
        ctx.setTimeoutDiscard(userProcessor.timeoutDiscard());
        if (userProcessor.processInIOThread()) {
            if (!deserializeRequestCommand(ctx, cmd, RpcDeserializeLevel.DESERIALIZE_ALL)) {
                return;
            }
            new ProcessTask(ctx, cmd).run();
            return;
        }
        Executor executor;
        if (null == userProcessor.getExecutorSelector()) {
            executor = userProcessor.getExecutor();
        } else {
            if (!deserializeRequestCommand(ctx, cmd, RpcDeserializeLevel.DESERIALIZE_HEADER)) {
                return;
            }
            executor = userProcessor.getExecutorSelector().select(cmd.getRequestClass(), cmd.getRequestHeader());
        }
        if (executor == null) {
            executor = (this.getExecutor() == null ? defaultExecutor : this.getExecutor());
        }
        executor.execute(new ProcessTask(ctx, cmd));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void doProcess(final RemotingContext ctx, RpcRequestCommand cmd) throws Exception {
        long currentTimestamp = System.currentTimeMillis();
        preProcessRemotingContext(ctx, cmd, currentTimestamp);
        if (ctx.isTimeoutDiscard() && ctx.isRequestTimeout()) {
            timeoutLog(cmd, currentTimestamp, ctx);
            return;
        }
        debugLog(ctx, cmd, currentTimestamp);
        if (!deserializeRequestCommand(ctx, cmd, RpcDeserializeLevel.DESERIALIZE_ALL)) {
            return;
        }
        dispatchToUserProcessor(ctx, cmd);
    }

    public void sendResponseIfNecessary(final RemotingContext ctx, byte type, final RemotingCommand response) {
        final int id = response.getId();
        if (type != RpcCommandType.REQUEST_ONEWAY) {
            RemotingCommand serializedResponse = response;
            try {
                response.serialize();
            } catch (SerializationException e) {
                String errMsg = "SerializationException occurred when sendResponseIfNecessary in RpcRequestProcessor, id=" + id;
                logger.error(errMsg, e);
                serializedResponse = this.getCommandFactory().createExceptionResponse(id, ResponseStatus.SERVER_SERIAL_EXCEPTION, e);
                try {
                    serializedResponse.serialize();
                } catch (SerializationException e1) {
                    logger.error("serialize SerializationException response failed!");
                }
            } catch (Throwable t) {
                String errMsg = "Serialize RpcResponseCommand failed when sendResponseIfNecessary in RpcRequestProcessor, id=" + id;
                logger.error(errMsg, t);
                serializedResponse = this.getCommandFactory().createExceptionResponse(id, t, errMsg);
            }
            ctx.writeAndFlush(serializedResponse).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Rpc response sent! requestId=" + id + ". The address is " + RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel()));
                    }
                    if (!future.isSuccess()) {
                        logger.error("Rpc response send failed! id=" + id + ". The address is " + RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel()), future.cause());
                    }
                }
            });
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Oneway rpc request received, do not send response, id=" + id + ", the address is " + RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel()));
            }
        }
    }

    private void dispatchToUserProcessor(RemotingContext ctx, RpcRequestCommand cmd) {
        final int id = cmd.getId();
        final byte type = cmd.getType();
        UserProcessor processor = ctx.getUserProcessor(cmd.getRequestClass());
        if (processor instanceof AsyncUserProcessor) {
            try {
                processor.handleRequest(processor.preHandleRequest(ctx, cmd.getRequestObject()), new RpcAsyncContext(ctx, cmd, this), cmd.getRequestObject());
            } catch (RejectedExecutionException e) {
                logger.warn("RejectedExecutionException occurred when do ASYNC process in RpcRequestProcessor");
                sendResponseIfNecessary(ctx, type, this.getCommandFactory().createExceptionResponse(id, ResponseStatus.SERVER_THREADPOOL_BUSY));
            } catch (Throwable t) {
                String errMsg = "AYSNC process rpc request failed in RpcRequestProcessor, id=" + id;
                logger.error(errMsg, t);
                sendResponseIfNecessary(ctx, type, this.getCommandFactory().createExceptionResponse(id, t, errMsg));
            }
        } else {
            try {
                Object responseObject = processor.handleRequest(processor.preHandleRequest(ctx, cmd.getRequestObject()), cmd.getRequestObject());
                sendResponseIfNecessary(ctx, type, this.getCommandFactory().createResponse(responseObject, cmd));
            } catch (RejectedExecutionException e) {
                logger.warn("RejectedExecutionException occurred when do SYNC process in RpcRequestProcessor");
                sendResponseIfNecessary(ctx, type, this.getCommandFactory().createExceptionResponse(id, ResponseStatus.SERVER_THREADPOOL_BUSY));
            } catch (Throwable t) {
                String errMsg = "SYNC process rpc request failed in RpcRequestProcessor, id=" + id;
                logger.error(errMsg, t);
                sendResponseIfNecessary(ctx, type, this.getCommandFactory().createExceptionResponse(id, t, errMsg));
            }
        }
    }

    private boolean deserializeRequestCommand(RemotingContext ctx, RpcRequestCommand cmd, int level) {
        boolean result;
        try {
            cmd.deserialize(level);
            result = true;
        } catch (DeserializationException e) {
            logger.error("DeserializationException occurred when process in RpcRequestProcessor, id={}, deserializeLevel={}", cmd.getId(), RpcDeserializeLevel.valueOf(level), e);
            sendResponseIfNecessary(ctx, cmd.getType(), this.getCommandFactory().createExceptionResponse(cmd.getId(), ResponseStatus.SERVER_DESERIAL_EXCEPTION, e));
            result = false;
        } catch (Throwable t) {
            String errMsg = "Deserialize RpcRequestCommand failed in RpcRequestProcessor, id=" + cmd.getId() + ", deserializeLevel=" + level;
            logger.error(errMsg, t);
            sendResponseIfNecessary(ctx, cmd.getType(), this.getCommandFactory().createExceptionResponse(cmd.getId(), t, errMsg));
            result = false;
        }
        return result;
    }

    private void preProcessRemotingContext(RemotingContext ctx, RpcRequestCommand cmd, long currentTimestamp) {
        ctx.setArriveTimestamp(cmd.getArriveTime());
        ctx.setTimeout(cmd.getTimeout());
        ctx.setRpcCommandType(cmd.getType());
        ctx.getInvokeContext().putIfAbsent(InvokeContext.BOLT_PROCESS_WAIT_TIME, currentTimestamp - cmd.getArriveTime());
    }

    private void timeoutLog(final RpcRequestCommand cmd, long currentTimestamp, RemotingContext ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug("request id [{}] currenTimestamp [{}] - arriveTime [{}] = server cost [{}] >= timeout value [{}].", cmd.getId(), currentTimestamp, cmd.getArriveTime(), (currentTimestamp - cmd.getArriveTime()), cmd.getTimeout());
        }
        String remoteAddr = "UNKNOWN";
        if (null != ctx) {
            ChannelHandlerContext channelCtx = ctx.getChannelContext();
            Channel channel = channelCtx.channel();
            if (null != channel) {
                remoteAddr = RemotingUtil.parseRemoteAddress(channel);
            }
        }
        logger.warn("Rpc request id[{}], from remoteAddr[{}] stop process, total wait time in queue is [{}], client timeout setting is [{}].", cmd.getId(), remoteAddr, (currentTimestamp - cmd.getArriveTime()), cmd.getTimeout());
    }

    private void debugLog(RemotingContext ctx, RpcRequestCommand cmd, long currentTimestamp) {
        if (logger.isDebugEnabled()) {
            logger.debug("Rpc request received! requestId={}, from {}", cmd.getId(), RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel()));
            logger.debug("request id {} currenTimestamp {} - arriveTime {} = server cost {} < timeout {}.", cmd.getId(), currentTimestamp, cmd.getArriveTime(), (currentTimestamp - cmd.getArriveTime()), cmd.getTimeout());
        }
    }

    class ProcessTask implements Runnable {

        RemotingContext ctx;

        RpcRequestCommand msg;

        public ProcessTask(RemotingContext ctx, RpcRequestCommand msg) {
            this.ctx = ctx;
            this.msg = msg;
        }

        @Override
        public void run() {
            try {
                RpcRequestProcessor.this.doProcess(ctx, msg);
            } catch (Throwable e) {
                String remotingAddress = RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel());
                logger.error("Exception caught when process rpc request command in RpcRequestProcessor, Id=" + msg.getId() + "! Invoke source address is [" + remotingAddress + "].", e);
            }
        }

    }

}
