package com.alipay.remoting;

import com.alipay.remoting.config.switches.ProtocolSwitch;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;

import java.io.Serializable;

public interface RemotingCommand extends Serializable {

    ProtocolCode getProtocolCode();

    CommandCode getCmdCode();

    int getId();

    InvokeContext getInvokeContext();

    byte getSerializer();

    ProtocolSwitch getProtocolSwitch();

    void serialize() throws SerializationException;

    void deserialize() throws DeserializationException;

    void serializeContent(InvokeContext invokeContext) throws SerializationException;

    void deserializeContent(InvokeContext invokeContext) throws DeserializationException;

}
