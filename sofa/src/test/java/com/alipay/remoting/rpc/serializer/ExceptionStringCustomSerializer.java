package com.alipay.remoting.rpc.serializer;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.ResponseCommand;

import java.util.concurrent.atomic.AtomicBoolean;

public class ExceptionStringCustomSerializer extends DefaultCustomSerializer {

    private AtomicBoolean serialFlag = new AtomicBoolean();

    private AtomicBoolean deserialFlag = new AtomicBoolean();

    private boolean serialException = false;

    private boolean serialRuntimeException = true;

    private boolean deserialException = false;

    private boolean deserialRuntimeException = true;

    public ExceptionStringCustomSerializer(boolean serialException, boolean deserialException) {
        this.serialException = serialException;
        this.deserialException = deserialException;
    }

    public ExceptionStringCustomSerializer(boolean serialException, boolean serialRuntimeException, boolean deserialException, boolean deserialRuntimeException) {
        this.serialException = serialException;
        this.serialRuntimeException = serialRuntimeException;
        this.deserialException = deserialException;
        this.deserialRuntimeException = deserialRuntimeException;
    }

    @Override
    public <T extends ResponseCommand> boolean serializeContent(T response) throws SerializationException {
        serialFlag.set(true);
        if (serialRuntimeException) {
            throw new RuntimeException("serialRuntimeException in ExceptionStringCustomSerializer!");
        } else if (serialException) {
            throw new SerializationException("serialException in ExceptionStringCustomSerializer!");
        } else {
            return false;
        }
    }

    @Override
    public <T extends ResponseCommand> boolean deserializeContent(T response, InvokeContext invokeContext) throws DeserializationException {
        deserialFlag.set(true);
        if (deserialRuntimeException) {
            throw new RuntimeException("deserialRuntimeException in ExceptionStringCustomSerializer!");
        } else if (deserialException) {
            throw new DeserializationException("deserialException in ExceptionStringCustomSerializer!");
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
