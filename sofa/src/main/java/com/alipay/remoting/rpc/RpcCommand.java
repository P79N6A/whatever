package com.alipay.remoting.rpc;

import com.alipay.remoting.CommandCode;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.ProtocolCode;
import com.alipay.remoting.RemotingCommand;
import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.config.switches.ProtocolSwitch;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.protocol.RpcDeserializeLevel;
import com.alipay.remoting.rpc.protocol.RpcProtocol;

public abstract class RpcCommand implements RemotingCommand {

    private static final long serialVersionUID = -3570261012462596503L;

    private CommandCode cmdCode;

    private byte version = 0x1;

    private byte type;

    private byte serializer = ConfigManager.serializer;

    private ProtocolSwitch protocolSwitch = new ProtocolSwitch();

    private int id;

    private short clazzLength = 0;

    private short headerLength = 0;

    private int contentLength = 0;

    private byte[] clazz;

    private byte[] header;

    private byte[] content;

    private InvokeContext invokeContext;

    public RpcCommand() {
    }

    public RpcCommand(byte type) {
        this();
        this.type = type;
    }

    public RpcCommand(CommandCode cmdCode) {
        this();
        this.cmdCode = cmdCode;
    }

    public RpcCommand(byte type, CommandCode cmdCode) {
        this(cmdCode);
        this.type = type;
    }

    public RpcCommand(byte version, byte type, CommandCode cmdCode) {
        this(type, cmdCode);
        this.version = version;
    }

    @Override
    public void serialize() throws SerializationException {
        // 序列化类名
        this.serializeClazz();
        // 序列化头部
        this.serializeHeader(this.invokeContext);
        // 序列化内容
        this.serializeContent(this.invokeContext);
    }

    @Override
    public void deserialize() throws DeserializationException {
        this.deserializeClazz();
        this.deserializeHeader(this.invokeContext);
        this.deserializeContent(this.invokeContext);
    }

    public void deserialize(long mask) throws DeserializationException {
        if (mask <= RpcDeserializeLevel.DESERIALIZE_CLAZZ) {
            this.deserializeClazz();
        } else if (mask <= RpcDeserializeLevel.DESERIALIZE_HEADER) {
            this.deserializeClazz();
            this.deserializeHeader(this.getInvokeContext());
        } else if (mask <= RpcDeserializeLevel.DESERIALIZE_ALL) {
            this.deserialize();
        }
    }

    public void serializeClazz() throws SerializationException {
    }

    public void deserializeClazz() throws DeserializationException {
    }

    public void serializeHeader(InvokeContext invokeContext) throws SerializationException {
    }

    @Override
    public void serializeContent(InvokeContext invokeContext) throws SerializationException {
    }

    public void deserializeHeader(InvokeContext invokeContext) throws DeserializationException {
    }

    @Override
    public void deserializeContent(InvokeContext invokeContext) throws DeserializationException {
    }

    @Override
    public ProtocolCode getProtocolCode() {
        return ProtocolCode.fromBytes(RpcProtocol.PROTOCOL_CODE);
    }

    @Override
    public CommandCode getCmdCode() {
        return cmdCode;
    }

    @Override
    public InvokeContext getInvokeContext() {
        return invokeContext;
    }

    @Override
    public byte getSerializer() {
        return serializer;
    }

    @Override
    public ProtocolSwitch getProtocolSwitch() {
        return protocolSwitch;
    }

    public void setCmdCode(CommandCode cmdCode) {
        this.cmdCode = cmdCode;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public void setSerializer(byte serializer) {
        this.serializer = serializer;
    }

    public void setProtocolSwitch(ProtocolSwitch protocolSwitch) {
        this.protocolSwitch = protocolSwitch;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte[] getHeader() {
        return header;
    }

    public void setHeader(byte[] header) {
        if (header != null) {
            this.header = header;
            this.headerLength = (short) header.length;
        }
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        if (content != null) {
            this.content = content;
            this.contentLength = content.length;
        }
    }

    public short getHeaderLength() {
        return headerLength;
    }

    public int getContentLength() {
        return contentLength;
    }

    public short getClazzLength() {
        return clazzLength;
    }

    public byte[] getClazz() {
        return clazz;
    }

    public void setClazz(byte[] clazz) {
        if (clazz != null) {
            this.clazz = clazz;
            this.clazzLength = (short) clazz.length;
        }
    }

    public void setInvokeContext(InvokeContext invokeContext) {
        this.invokeContext = invokeContext;
    }

}
