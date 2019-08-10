package com.alipay.remoting.rpc.serializer;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.alipay.remoting.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalStringCustomSerializer_InvokeContext extends DefaultCustomSerializer {

    private AtomicBoolean serialFlag = new AtomicBoolean();

    private AtomicBoolean deserialFlag = new AtomicBoolean();

    public static final String UNIVERSAL_RESP = "UNIVERSAL RESPONSE";

    public static final String SERIALTYPE_KEY = "serial.type";

    public static final String SERIALTYPE1_value = "SERIAL1";

    public static final String SERIALTYPE2_value = "SERIAL2";

    @Override
    public <T extends ResponseCommand> boolean serializeContent(T response) throws SerializationException {
        serialFlag.set(true);
        RpcResponseCommand rpcResp = (RpcResponseCommand) response;
        String str = (String) rpcResp.getResponseObject();
        try {
            rpcResp.setContent(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public <T extends ResponseCommand> boolean deserializeContent(T response, InvokeContext invokeContext) throws DeserializationException {
        deserialFlag.set(true);
        RpcResponseCommand rpcResp = (RpcResponseCommand) response;
        if (StringUtils.equals(SERIALTYPE1_value, (String) invokeContext.get(SERIALTYPE_KEY))) {
            try {
                rpcResp.setResponseObject(new String(rpcResp.getContent(), "UTF-8") + "RANDOM");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            rpcResp.setResponseObject(UNIVERSAL_RESP);
        }
        return true;
    }

    public boolean isSerialized() {
        return this.serialFlag.get();
    }

    public boolean isDeserialized() {
        return this.deserialFlag.get();
    }

}
