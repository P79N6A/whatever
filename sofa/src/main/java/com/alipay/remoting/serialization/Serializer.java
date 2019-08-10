package com.alipay.remoting.serialization;

import com.alipay.remoting.exception.CodecException;

public interface Serializer {

    byte[] serialize(final Object obj) throws CodecException;

    <T> T deserialize(final byte[] data, String classOfT) throws CodecException;

}
