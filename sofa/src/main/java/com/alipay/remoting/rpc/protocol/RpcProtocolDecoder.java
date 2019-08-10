package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.codec.ProtocolCodeBasedDecoder;
import io.netty.buffer.ByteBuf;

public class RpcProtocolDecoder extends ProtocolCodeBasedDecoder {
    public static final int MIN_PROTOCOL_CODE_WITH_VERSION = 2;

    public RpcProtocolDecoder(int protocolCodeLength) {
        super(protocolCodeLength);
    }

    @Override
    protected byte decodeProtocolVersion(ByteBuf in) {
        in.resetReaderIndex();
        if (in.readableBytes() >= protocolCodeLength + DEFAULT_PROTOCOL_VERSION_LENGTH) {
            byte rpcProtocolCodeByte = in.readByte();
            if (rpcProtocolCodeByte >= MIN_PROTOCOL_CODE_WITH_VERSION) {
                return in.readByte();
            }
        }
        return DEFAULT_ILLEGAL_PROTOCOL_VERSION_LENGTH;
    }

}