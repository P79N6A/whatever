package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.CustomSerializer;
import com.alipay.remoting.CustomSerializerManager;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.config.Configs;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.serialization.SerializerManager;

import java.io.UnsupportedEncodingException;

public class RpcResponseCommand extends ResponseCommand {

    private static final long serialVersionUID = 5667111367880018776L;

    private Object responseObject;

    private String responseClass;

    private CustomSerializer customSerializer;

    private Object responseHeader;

    private String errorMsg;

    public RpcResponseCommand() {
        super(RpcCommandCode.RPC_RESPONSE);
    }

    public RpcResponseCommand(Object response) {
        super(RpcCommandCode.RPC_RESPONSE);
        this.responseObject = response;
    }

    public RpcResponseCommand(int id, Object response) {
        super(RpcCommandCode.RPC_RESPONSE, id);
        this.responseObject = response;
    }

    public Object getResponseObject() {
        return responseObject;
    }

    public void setResponseObject(Object response) {
        this.responseObject = response;
    }

    @Override
    public void serializeClazz() throws SerializationException {
        if (this.getResponseClass() != null) {
            try {
                byte[] clz = this.getResponseClass().getBytes(Configs.DEFAULT_CHARSET);
                this.setClazz(clz);
            } catch (UnsupportedEncodingException e) {
                throw new SerializationException("Unsupported charset: " + Configs.DEFAULT_CHARSET, e);
            }
        }
    }

    @Override
    public void deserializeClazz() throws DeserializationException {
        if (this.getClazz() != null && this.getResponseClass() == null) {
            try {
                this.setResponseClass(new String(this.getClazz(), Configs.DEFAULT_CHARSET));
            } catch (UnsupportedEncodingException e) {
                throw new DeserializationException("Unsupported charset: " + Configs.DEFAULT_CHARSET, e);
            }
        }
    }

    @Override
    public void serializeContent(InvokeContext invokeContext) throws SerializationException {
        if (this.getResponseObject() != null) {
            try {
                if (this.getCustomSerializer() != null && this.getCustomSerializer().serializeContent(this)) {
                    return;
                }
                this.setContent(SerializerManager.getSerializer(this.getSerializer()).serialize(this.responseObject));
            } catch (SerializationException e) {
                throw e;
            } catch (Exception e) {
                throw new SerializationException("Exception caught when serialize content of rpc response command!", e);
            }
        }
    }

    @Override
    public void deserializeContent(InvokeContext invokeContext) throws DeserializationException {
        if (this.getResponseObject() == null) {
            try {
                if (this.getCustomSerializer() != null && this.getCustomSerializer().deserializeContent(this, invokeContext)) {
                    return;
                }
                if (this.getContent() != null) {
                    this.setResponseObject(SerializerManager.getSerializer(this.getSerializer()).deserialize(this.getContent(), this.responseClass));
                }
            } catch (DeserializationException e) {
                throw e;
            } catch (Exception e) {
                throw new DeserializationException("Exception caught when deserialize content of rpc response command!", e);
            }
        }

    }

    @Override
    public void serializeHeader(InvokeContext invokeContext) throws SerializationException {
        if (this.getCustomSerializer() != null) {
            try {
                this.getCustomSerializer().serializeHeader(this);
            } catch (SerializationException e) {
                throw e;
            } catch (Exception e) {
                throw new SerializationException("Exception caught when serialize header of rpc response command!", e);
            }
        }
    }

    @Override
    public void deserializeHeader(InvokeContext invokeContext) throws DeserializationException {
        if (this.getHeader() != null && this.getResponseHeader() == null) {
            if (this.getCustomSerializer() != null) {
                try {
                    this.getCustomSerializer().deserializeHeader(this, invokeContext);
                } catch (DeserializationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DeserializationException("Exception caught when deserialize header of rpc response command!", e);
                }
            }
        }
    }

    public String getResponseClass() {
        return responseClass;
    }

    public void setResponseClass(String responseClass) {
        this.responseClass = responseClass;
    }

    public Object getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(Object responseHeader) {
        this.responseHeader = responseHeader;
    }

    public CustomSerializer getCustomSerializer() {
        if (this.customSerializer != null) {
            return customSerializer;
        }
        if (this.responseClass != null) {
            this.customSerializer = CustomSerializerManager.getCustomSerializer(this.responseClass);
        }
        if (this.customSerializer == null) {
            this.customSerializer = CustomSerializerManager.getCustomSerializer(this.getCmdCode());
        }
        return this.customSerializer;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

}
