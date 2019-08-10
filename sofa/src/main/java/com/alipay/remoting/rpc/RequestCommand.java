package com.alipay.remoting.rpc;

import com.alipay.remoting.CommandCode;

public abstract class RequestCommand extends RpcCommand {

    private static final long serialVersionUID = -3457717009326601317L;

    private int timeout = -1;

    public RequestCommand() {
        super(RpcCommandType.REQUEST);
    }

    public RequestCommand(CommandCode code) {
        super(RpcCommandType.REQUEST, code);
    }

    public RequestCommand(byte type, CommandCode code) {
        super(type, code);
    }

    public RequestCommand(byte version, byte type, CommandCode code) {
        super(version, type, code);
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

}
