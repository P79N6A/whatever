package com.alipay.remoting.rpc.serializer;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;

import java.util.concurrent.atomic.AtomicBoolean;

public class ExceptionRequestBodyCustomSerializer extends DefaultCustomSerializer {

    private AtomicBoolean serialFlag = new AtomicBoolean();

    private AtomicBoolean deserialFlag = new AtomicBoolean();

    private boolean serialException = false;

    private boolean serialRuntimeException = true;

    private boolean deserialException = false;

    private boolean deserialRuntimeException = true;

    public ExceptionRequestBodyCustomSerializer(boolean serialRuntimeException, boolean deserialRuntimeException) {
        this.serialRuntimeException = serialRuntimeException;
        this.deserialRuntimeException = deserialRuntimeException;
    }

    public ExceptionRequestBodyCustomSerializer(boolean serialException, boolean serialRuntimeException, boolean deserialException, boolean deserialRuntimeException) {
        this.serialException = serialException;
        this.serialRuntimeException = serialRuntimeException;
        this.deserialException = deserialException;
        this.deserialRuntimeException = deserialRuntimeException;
    }

    @Override
    public <T extends RequestCommand> boolean serializeContent(T req, InvokeContext invokeContext) throws SerializationException {
        serialFlag.set(true);
        if (serialRuntimeException) {
            throw new RuntimeException("serialRuntimeException in ExceptionRequestBodyCustomSerializer!");
        } else if (serialException) {
            throw new SerializationException("serialException in ExceptionRequestBodyCustomSerializer!");
        } else {
            return false;
        }
    }

    @Override
    public <T extends RequestCommand> boolean deserializeContent(T req) throws DeserializationException {
        deserialFlag.set(true);
        if (deserialRuntimeException) {
            throw new RuntimeException("deserialRuntimeException in ExceptionRequestBodyCustomSerializer!");
        } else if (deserialException) {
            throw new DeserializationException("deserialException in ExceptionRequestBodyCustomSerializer!");
        } else {
            return false;
        }
    }

    public boolean isSerialized() {
        return this.serialFlag.get();
    }

    public boolean isDeserialized() {
        return this.deserialFlag.get();
    }

}
