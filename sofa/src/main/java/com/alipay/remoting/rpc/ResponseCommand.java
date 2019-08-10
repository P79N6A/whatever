package com.alipay.remoting.rpc;

import com.alipay.remoting.CommandCode;
import com.alipay.remoting.ResponseStatus;

import java.net.InetSocketAddress;

public class ResponseCommand extends RpcCommand {

    private static final long serialVersionUID = -5194754228565292441L;

    private ResponseStatus responseStatus;

    private long responseTimeMillis;

    private InetSocketAddress responseHost;

    private Throwable cause;

    public ResponseCommand() {
        super(RpcCommandType.RESPONSE);
    }

    public ResponseCommand(CommandCode code) {
        super(RpcCommandType.RESPONSE, code);
    }

    public ResponseCommand(int id) {
        super(RpcCommandType.RESPONSE);
        this.setId(id);
    }

    public ResponseCommand(CommandCode code, int id) {
        super(RpcCommandType.RESPONSE, code);
        this.setId(id);
    }

    public ResponseCommand(byte version, byte type, CommandCode code, int id) {
        super(version, type, code);
        this.setId(id);
    }

    public long getResponseTimeMillis() {
        return responseTimeMillis;
    }

    public void setResponseTimeMillis(long responseTimeMillis) {
        this.responseTimeMillis = responseTimeMillis;
    }

    public InetSocketAddress getResponseHost() {
        return responseHost;
    }

    public void setResponseHost(InetSocketAddress responseHost) {
        this.responseHost = responseHost;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

}
