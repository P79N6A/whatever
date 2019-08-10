package com.alipay.remoting.rpc.serializer;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.common.RequestBody;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalRequestBodyCustomSerializer extends DefaultCustomSerializer {

    private AtomicBoolean serialFlag = new AtomicBoolean();

    private AtomicBoolean deserialFlag = new AtomicBoolean();

    private byte contentSerializer = -1;

    private byte contentDeserializer = -1;

    @Override
    public <T extends RequestCommand> boolean serializeContent(T req, InvokeContext invokeContext) throws SerializationException {
        serialFlag.set(true);
        RpcRequestCommand rpcReq = (RpcRequestCommand) req;
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
        contentSerializer = rpcReq.getSerializer();
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
        contentDeserializer = rpcReq.getSerializer();
        return true;
    }

    public boolean isSerialized() {
        return this.serialFlag.get();
    }

    public boolean isDeserialized() {
        return this.deserialFlag.get();
    }

    public byte getContentSerializer() {
        return contentSerializer;
    }

    public byte getContentDeserializer() {
        return contentDeserializer;
    }

    public void reset() {
        this.contentDeserializer = -1;
        this.contentSerializer = -1;
        this.deserialFlag.set(false);
        this.serialFlag.set(false);
    }

}
