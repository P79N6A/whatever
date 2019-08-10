package com.alipay.remoting.rpc.userprocessor.executorselector;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;

import java.io.UnsupportedEncodingException;

import static com.alipay.remoting.rpc.userprocessor.executorselector.DefaultExecutorSelector.EXECUTOR1;

public class CustomHeaderSerializer extends DefaultCustomSerializer {

    @Override
    public <T extends RequestCommand> boolean serializeHeader(T request, InvokeContext invokeContext) throws SerializationException {
        if (request instanceof RpcRequestCommand) {
            RpcRequestCommand requestCommand = (RpcRequestCommand) request;
            try {
                requestCommand.setHeader(EXECUTOR1.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                System.err.println("UnsupportedEncodingException");
            }
            return true;
        }
        return false;
    }

    @Override
    public <T extends RequestCommand> boolean deserializeHeader(T request) throws DeserializationException {
        if (request instanceof RpcRequestCommand) {
            RpcRequestCommand requestCommand = (RpcRequestCommand) request;
            byte[] header = requestCommand.getHeader();
            try {
                requestCommand.setRequestHeader(new String(header, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                System.err.println("UnsupportedEncodingException");
            }
            return true;
        }
        return false;
    }

    @Override
    public <T extends ResponseCommand> boolean serializeHeader(T response) throws SerializationException {
        if (response instanceof RpcResponseCommand) {
            RpcResponseCommand responseCommand = (RpcResponseCommand) response;
            try {
                responseCommand.setHeader(EXECUTOR1.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                System.err.println("UnsupportedEncodingException");
            }
            return true;
        }
        return false;
    }

    @Override
    public <T extends ResponseCommand> boolean deserializeHeader(T response, InvokeContext invokeContext) throws DeserializationException {
        if (response instanceof RpcResponseCommand) {
            RpcResponseCommand responseCommand = (RpcResponseCommand) response;
            byte[] header = responseCommand.getHeader();
            try {
                responseCommand.setResponseHeader(new String(header, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                System.err.println("UnsupportedEncodingException");
            }
            return true;
        }
        return false;
    }

}