package com.alipay.remoting;

public enum CommonCommandCode implements CommandCode {

    HEARTBEAT(CommandCode.HEARTBEAT_VALUE);

    private short value;

    CommonCommandCode(short value) {
        this.value = value;
    }

    @Override
    public short value() {
        return this.value;
    }

    public static CommonCommandCode valueOf(short value) {
        switch (value) {
            case CommandCode.HEARTBEAT_VALUE:
                return HEARTBEAT;
        }
        throw new IllegalArgumentException("Unknown Rpc command code value ," + value);
    }

}