package com.alipay.remoting.rpc;

import com.alipay.remoting.*;
import com.alipay.remoting.config.switches.ProtocolSwitch;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.protocol.RpcProtocolManager;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.util.RemotingUtil;
import org.slf4j.Logger;

public abstract class RpcRemoting extends BaseRemoting {

    static {
        // 注册协议
        RpcProtocolManager.initProtocols();
    }

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    protected RemotingAddressParser addressParser;

    protected DefaultConnectionManager connectionManager;

    public RpcRemoting(CommandFactory commandFactory) {
        super(commandFactory);
    }

    public RpcRemoting(CommandFactory commandFactory, RemotingAddressParser addressParser, DefaultConnectionManager connectionManager) {
        this(commandFactory);
        this.addressParser = addressParser;
        this.connectionManager = connectionManager;
    }

    public void oneway(final String addr, final Object request, final InvokeContext invokeContext) throws RemotingException, InterruptedException {
        Url url = this.addressParser.parse(addr);
        this.oneway(url, request, invokeContext);
    }

    public abstract void oneway(final Url url, final Object request, final InvokeContext invokeContext) throws RemotingException, InterruptedException;

    public void oneway(final Connection conn, final Object request, final InvokeContext invokeContext) throws RemotingException {
        // 请求转为RequestCommand
        RequestCommand requestCommand = (RequestCommand) toRemotingCommand(request, conn, invokeContext, -1);
        // 请求类型，单向
        requestCommand.setType(RpcCommandType.REQUEST_ONEWAY);
        // 预处理InvokeContext
        preProcessInvokeContext(invokeContext, requestCommand, conn);
        // 直接writeAndFlush
        super.oneway(conn, requestCommand);
    }

    public Object invokeSync(final String addr, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException {
        Url url = this.addressParser.parse(addr);
        return this.invokeSync(url, request, invokeContext, timeoutMillis);
    }

    public abstract Object invokeSync(final Url url, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException;

    public Object invokeSync(final Connection conn, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException {
        RemotingCommand requestCommand = toRemotingCommand(request, conn, invokeContext, timeoutMillis);
        preProcessInvokeContext(invokeContext, requestCommand, conn);
        // 阻塞调用
        ResponseCommand responseCommand = (ResponseCommand) super.invokeSync(conn, requestCommand, timeoutMillis);
        responseCommand.setInvokeContext(invokeContext);
        // 反序列化结果
        Object responseObject = RpcResponseResolver.resolveResponseObject(responseCommand, RemotingUtil.parseRemoteAddress(conn.getChannel()));
        return responseObject;
    }

    public RpcResponseFuture invokeWithFuture(final String addr, final Object request, final InvokeContext invokeContext, int timeoutMillis) throws RemotingException, InterruptedException {
        Url url = this.addressParser.parse(addr);
        return this.invokeWithFuture(url, request, invokeContext, timeoutMillis);
    }

    public abstract RpcResponseFuture invokeWithFuture(final Url url, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException;

    public RpcResponseFuture invokeWithFuture(final Connection conn, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException {
        RemotingCommand requestCommand = toRemotingCommand(request, conn, invokeContext, timeoutMillis);
        preProcessInvokeContext(invokeContext, requestCommand, conn);
        InvokeFuture future = super.invokeWithFuture(conn, requestCommand, timeoutMillis);
        return new RpcResponseFuture(RemotingUtil.parseRemoteAddress(conn.getChannel()), future);
    }

    public void invokeWithCallback(String addr, Object request, final InvokeContext invokeContext, InvokeCallback invokeCallback, int timeoutMillis) throws RemotingException, InterruptedException {
        Url url = this.addressParser.parse(addr);
        this.invokeWithCallback(url, request, invokeContext, invokeCallback, timeoutMillis);
    }

    public abstract void invokeWithCallback(final Url url, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException;

    public void invokeWithCallback(final Connection conn, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException {
        RemotingCommand requestCommand = toRemotingCommand(request, conn, invokeContext, timeoutMillis);
        preProcessInvokeContext(invokeContext, requestCommand, conn);
        super.invokeWithCallback(conn, requestCommand, invokeCallback, timeoutMillis);
    }

    protected RemotingCommand toRemotingCommand(Object request, Connection conn, InvokeContext invokeContext, int timeoutMillis) throws SerializationException {

        // 通过命令工厂将Request转为Command
        RpcRequestCommand command = this.getCommandFactory().createRequestCommand(request);
        if (null != invokeContext) {

            Object clientCustomSerializer = invokeContext.get(InvokeContext.BOLT_CUSTOM_SERIALIZER);
            if (null != clientCustomSerializer) {
                try {
                    // 序列化器
                    command.setSerializer((Byte) clientCustomSerializer);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Illegal custom serializer [" + clientCustomSerializer + "], the type of value should be [byte], but now is [" + clientCustomSerializer.getClass().getName() + "].");
                }
            }

            Boolean crcSwitch = invokeContext.get(InvokeContext.BOLT_CRC_SWITCH, ProtocolSwitch.CRC_SWITCH_DEFAULT_VALUE);
            if (null != crcSwitch && crcSwitch) {
                // CRC校验
                command.setProtocolSwitch(ProtocolSwitch.create(new int[]{ProtocolSwitch.CRC_SWITCH_INDEX}));
            }
        } else {
            // 协议
            command.setProtocolSwitch(ProtocolSwitch.create(new int[]{ProtocolSwitch.CRC_SWITCH_INDEX}));
        }
        // 超时
        command.setTimeout(timeoutMillis);
        // 请求类名
        command.setRequestClass(request.getClass().getName());
        // 上下文
        command.setInvokeContext(invokeContext);
        // 序列化Command
        command.serialize();
        logDebugInfo(command);
        return command;
    }

    protected abstract void preProcessInvokeContext(InvokeContext invokeContext, RemotingCommand cmd, Connection connection);

    private void logDebugInfo(RemotingCommand requestCommand) {
        if (logger.isDebugEnabled()) {
            logger.debug("Send request, requestId=" + requestCommand.getId());
        }
    }

    @Override
    protected InvokeFuture createInvokeFuture(RemotingCommand request, InvokeContext invokeContext) {
        return new DefaultInvokeFuture(request.getId(), null, null, request.getProtocolCode().getFirstByte(), this.getCommandFactory(), invokeContext);
    }

    @Override
    protected InvokeFuture createInvokeFuture(Connection conn, RemotingCommand request, InvokeContext invokeContext, InvokeCallback invokeCallback) {
        return new DefaultInvokeFuture(request.getId(), new RpcInvokeCallbackListener(RemotingUtil.parseRemoteAddress(conn.getChannel())), invokeCallback, request.getProtocolCode().getFirstByte(), this.getCommandFactory(), invokeContext);
    }

}
