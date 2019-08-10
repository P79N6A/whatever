package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.CommandCode;

public enum RpcCommandCode implements CommandCode {

    RPC_REQUEST((short) 1), RPC_RESPONSE((short) 2);

    private short value;

    RpcCommandCode(short value) {
        this.value = value;
    }

    @Override
    public short value() {
        return this.value;
    }

    public static RpcCommandCode valueOf(short value) {
        switch (value) {
            case 1:
                return RPC_REQUEST;
            case 2:
                return RPC_RESPONSE;
        }
        throw new IllegalArgumentException("Unknown Rpc command code value: " + value);
    }

}