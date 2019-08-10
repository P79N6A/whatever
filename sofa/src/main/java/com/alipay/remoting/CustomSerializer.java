package com.alipay.remoting;

import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.ResponseCommand;

public interface CustomSerializer {

    <T extends RequestCommand> boolean serializeHeader(T request, InvokeContext invokeContext) throws SerializationException;

    <T extends ResponseCommand> boolean serializeHeader(T response) throws SerializationException;

    <T extends RequestCommand> boolean deserializeHeader(T request) throws DeserializationException;

    <T extends ResponseCommand> boolean deserializeHeader(T response, InvokeContext invokeContext) throws DeserializationException;

    <T extends RequestCommand> boolean serializeContent(T request, InvokeContext invokeContext) throws SerializationException;

    <T extends ResponseCommand> boolean serializeContent(T response) throws SerializationException;

    <T extends RequestCommand> boolean deserializeContent(T request) throws DeserializationException;

    <T extends ResponseCommand> boolean deserializeContent(T response, InvokeContext invokeContext) throws DeserializationException;

}