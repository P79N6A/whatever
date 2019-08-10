package com.alipay.remoting.serialization;

public class SerializerManager {

    private static Serializer[] serializers = new Serializer[5];

    public static final byte Hessian2 = 1;

    static {
        addSerializer(Hessian2, new HessianSerializer());
    }

    public static Serializer getSerializer(int idx) {
        return serializers[idx];
    }

    public static void addSerializer(int idx, Serializer serializer) {
        if (serializers.length <= idx) {
            Serializer[] newSerializers = new Serializer[idx + 5];
            System.arraycopy(serializers, 0, newSerializers, 0, serializers.length);
            serializers = newSerializers;
        }
        serializers[idx] = serializer;
    }

}
