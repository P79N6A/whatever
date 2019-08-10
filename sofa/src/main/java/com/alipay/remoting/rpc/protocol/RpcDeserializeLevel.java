package com.alipay.remoting.rpc.protocol;

public class RpcDeserializeLevel {

    public final static int DESERIALIZE_ALL = 0x02;

    public final static int DESERIALIZE_HEADER = 0x01;

    public final static int DESERIALIZE_CLAZZ = 0x00;

    public static String valueOf(int value) {
        switch (value) {
            case 0x00:
                return "DESERIALIZE_CLAZZ";
            case 0x01:
                return "DESERIALIZE_HEADER";
            case 0x02:
                return "DESERIALIZE_ALL";
        }
        throw new IllegalArgumentException("Unknown deserialize level value ," + value);
    }

}