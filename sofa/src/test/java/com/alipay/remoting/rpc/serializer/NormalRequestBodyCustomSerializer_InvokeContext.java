package com.alipay.remoting.rpc.serializer;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.common.RequestBody;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalRequestBodyCustomSerializer_InvokeContext extends DefaultCustomSerializer {

    private AtomicBoolean serialFlag = new AtomicBoolean();

    private AtomicBoolean deserialFlag = new AtomicBoolean();

    public static final String UNIVERSAL_REQ = "UNIVERSAL REQUEST";

    public static final String SERIALTYPE_KEY = "serial.type";

    public static final String SERIALTYPE1_value = "SERIAL1";

    public static final String SERIALTYPE2_value = "SERIAL2";

    @Override
    public <T extends RequestCommand> boolean serializeContent(T req, InvokeContext invokeContext) throws SerializationException {
        serialFlag.set(true);
        RpcRequestCommand rpcReq = (RpcRequestCommand) req;
        if (StringUtils.equals(SERIALTYPE1_value, (String) invokeContext.get(SERIALTYPE_KEY))) {
            RequestBody bd = (RequestBody) rpcReq.getRequestObject();
            int id = bd.getId();
            byte[] msg;
            try {
                msg = bd.getMsg().getBytes("UTF-8");
                ByteBuffer bb = ByteBuffer.allocate(4 + msg.length);
                bb.putInt(id);
                bb.put(msg);
                rpcReq.setContent(bb.array());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                rpcReq.setContent(UNIVERSAL_REQ.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public <T extends RequestCommand> boolean deserializeContent(T req) throws DeserializationException {
        deserialFlag.set(true);
        RpcRequestCommand rpcReq = (RpcRequestCommand) req;
        byte[] content = rpcReq.getContent();
        ByteBuffer bb = ByteBuffer.wrap(content);
        int a = bb.getInt();
        byte[] dst = new byte[content.length - 4];
        bb.get(dst, 0, dst.length);
        try {
            String b = new String(dst, "UTF-8");
            RequestBody bd = new RequestBody(a, b);
            rpcReq.setRequestObject(bd);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
