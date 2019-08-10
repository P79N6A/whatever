package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.*;
import com.alipay.remoting.rpc.RpcCommandFactory;

public class RpcProtocol implements Protocol {
    public static final byte PROTOCOL_CODE = (byte) 1;

    private static final int REQUEST_HEADER_LEN = 22;

    private static final int RESPONSE_HEADER_LEN = 20;

    private CommandEncoder encoder;

    private CommandDecoder decoder;

    private HeartbeatTrigger heartbeatTrigger;

    private CommandHandler commandHandler;

    private CommandFactory commandFactory;

    public RpcProtocol() {
        this.encoder = new RpcCommandEncoder();
        this.decoder = new RpcCommandDecoder();
        this.commandFactory = new RpcCommandFactory();
        this.heartbeatTrigger = new RpcHeartbeatTrigger(this.commandFactory);
        this.commandHandler = new RpcCommandHandler(this.commandFactory);
    }

    public static int getRequestHeaderLength() {
        return RpcProtocol.REQUEST_HEADER_LEN;
    }

    public static int getResponseHeaderLength() {
        return RpcProtocol.RESPONSE_HEADER_LEN;
    }

    @Override
    public CommandEncoder getEncoder() {
        return this.encoder;
    }

    @Override
    public CommandDecoder getDecoder() {
        return this.decoder;
    }

    @Override
    public HeartbeatTrigger getHeartbeatTrigger() {
        return this.heartbeatTrigger;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return this.commandHandler;
    }

    @Override
    public CommandFactory getCommandFactory() {
        return this.commandFactory;
    }

}
