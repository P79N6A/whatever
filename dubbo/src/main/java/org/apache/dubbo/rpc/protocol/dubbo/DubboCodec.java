package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.io.UnsafeByteArrayInputStream;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.codec.ExchangeCodec;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.*;
import static org.apache.dubbo.rpc.protocol.dubbo.CallbackServiceCodec.encodeInvocationArgument;

public class DubboCodec extends ExchangeCodec {

    public static final String NAME = "dubbo";

    public static final String DUBBO_VERSION = Version.getProtocolVersion();

    public static final byte RESPONSE_WITH_EXCEPTION = 0;

    public static final byte RESPONSE_VALUE = 1;

    public static final byte RESPONSE_NULL_VALUE = 2;

    public static final byte RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS = 3;

    public static final byte RESPONSE_VALUE_WITH_ATTACHMENTS = 4;

    public static final byte RESPONSE_NULL_VALUE_WITH_ATTACHMENTS = 5;

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    private static final Logger log = LoggerFactory.getLogger(DubboCodec.class);

    @Override
    protected Object decodeBody(Channel channel, InputStream is, byte[] header) throws IOException {
        // 获取消息头中的第三个字节，并通过逻辑与运算得到序列化器编号
        byte flag = header[2], proto = (byte) (flag & SERIALIZATION_MASK);
        // 获取调用编号
        long id = Bytes.bytes2long(header, 4);
        // 通过逻辑与运算得到调用类型，0 - Response，1 - Request
        if ((flag & FLAG_REQUEST) == 0) {
            // 对响应结果进行解码，得到Response对象
            // 创建Response对象
            Response res = new Response(id);
            // 检测事件标志位
            if ((flag & FLAG_EVENT) != 0) {
                res.setEvent(true);
            }
            byte status = header[3];
            res.setStatus(status);
            try {
                // 如果响应状态为OK，表明调用过程正常
                if (status == Response.OK) {
                    Object data;
                    if (res.isHeartbeat()) {
                        ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                        // 反序列化心跳数据，已废弃
                        data = decodeHeartbeatData(channel, in);
                    } else if (res.isEvent()) {
                        ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                        // 反序列化事件数据
                        data = decodeEventData(channel, in);
                    } else {
                        DecodeableRpcResult result;
                        // 根据url参数决定是否在IO线程上执行解码逻辑
                        if (channel.getUrl().getParameter(DECODE_IN_IO_THREAD_KEY, DEFAULT_DECODE_IN_IO_THREAD)) {
                            // 创建DecodeableRpcResult对象
                            result = new DecodeableRpcResult(channel, res, is, (Invocation) getRequestData(id), proto);
                            // 进行后续的解码工作
                            result.decode();
                        } else {
                            // 创建DecodeableRpcResult对象
                            result = new DecodeableRpcResult(channel, res, new UnsafeByteArrayInputStream(readMessageData(is)), (Invocation) getRequestData(id), proto);
                        }
                        data = result;
                    }
                    // 设置DecodeableRpcResult对象到Response对象中
                    res.setResult(data);
                }
                // 响应状态非OK，表明调用过程出现了异常
                else {
                    ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                    // 反序列化异常信息，并设置到Response对象中
                    res.setErrorMessage(in.readUTF());
                }
            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode response failed: " + t.getMessage(), t);
                }
                // 解码过程中出现了错误，此时设置CLIENT_ERROR状态码到Response对象中
                res.setStatus(Response.CLIENT_ERROR);
                res.setErrorMessage(StringUtils.toString(t));
            }
            return res;
        } else {
            // 创建Request对象
            Request req = new Request(id);
            req.setVersion(Version.getProtocolVersion());
            // 通过逻辑与运算得到通信方式，并设置到Request对象中
            req.setTwoWay((flag & FLAG_TWOWAY) != 0);
            // 通过位运算检测数据包是否为事件类型
            if ((flag & FLAG_EVENT) != 0) {
                // 设置心跳事件到Request对象中
                req.setEvent(true);
            }
            try {
                Object data;
                ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                if (req.isHeartbeat()) {
                    // 对心跳包进行解码，该方法已被标注为废弃
                    data = decodeHeartbeatData(channel, in);
                } else if (req.isEvent()) {
                    // 对事件数据进行解码
                    data = decodeEventData(channel, in);
                } else {
                    DecodeableRpcInvocation inv;
                    // 根据url参数判断是否在IO线程上对消息体进行解码
                    if (channel.getUrl().getParameter(DECODE_IN_IO_THREAD_KEY, DEFAULT_DECODE_IN_IO_THREAD)) {
                        inv = new DecodeableRpcInvocation(channel, req, is, proto);
                        // 在当前线程，也就是IO线程上进行后续的解码工作
                        // 此工作完成后，可将调用方法名、attachment、以及调用参数解析出来
                        inv.decode();
                    } else {
                        // 仅创建DecodeableRpcInvocation对象，但不在当前线程上执行解码逻辑
                        inv = new DecodeableRpcInvocation(channel, req, new UnsafeByteArrayInputStream(readMessageData(is)), proto);
                    }
                    data = inv;
                }
                // 设置data到Request对象中
                req.setData(data);
            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode request failed: " + t.getMessage(), t);
                }
                // 若解码过程中出现异常，则将broken字段设为true，并将异常对象设置到Reqeust对象中
                req.setBroken(true);
                req.setData(t);
            }
            return req;
        }
    }

    private byte[] readMessageData(InputStream is) throws IOException {
        if (is.available() > 0) {
            byte[] result = new byte[is.available()];
            is.read(result);
            return result;
        }
        return new byte[]{};
    }

    @Override
    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(channel, out, data, DUBBO_VERSION);
    }

    @Override
    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeResponseData(channel, out, data, DUBBO_VERSION);
    }

    @Override
    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data, String version) throws IOException {
        RpcInvocation inv = (RpcInvocation) data;
        // 依次序列化dubbo version、path、version
        out.writeUTF(version);
        out.writeUTF(inv.getAttachment(PATH_KEY));
        out.writeUTF(inv.getAttachment(VERSION_KEY));
        // 序列化调用方法名
        out.writeUTF(inv.getMethodName());
        // 将参数类型转换为字符串，并进行序列化
        out.writeUTF(ReflectUtils.getDesc(inv.getParameterTypes()));
        Object[] args = inv.getArguments();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                // 对运行时参数进行序列化
                out.writeObject(encodeInvocationArgument(channel, inv, i));
            }
        }
        // 序列化attachments
        out.writeObject(RpcUtils.getNecessaryAttachments(inv));
    }

    @Override
    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data, String version) throws IOException {
        Result result = (Result) data;
        // 检测当前协议版本是否支持带有attachment集合的Response对象
        boolean attach = Version.isSupportResponseAttachment(version);
        Throwable th = result.getException();
        // 异常信息为空
        if (th == null) {
            Object ret = result.getValue();
            // 调用结果为空
            if (ret == null) {
                // 序列化响应类型
                out.writeByte(attach ? RESPONSE_NULL_VALUE_WITH_ATTACHMENTS : RESPONSE_NULL_VALUE);
            }
            // 调用结果非空
            else {
                // 序列化响应类型
                out.writeByte(attach ? RESPONSE_VALUE_WITH_ATTACHMENTS : RESPONSE_VALUE);
                // 序列化调用结果
                out.writeObject(ret);
            }
        }
        // 异常信息非空
        else {
            // 序列化响应类型
            out.writeByte(attach ? RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS : RESPONSE_WITH_EXCEPTION);
            // 序列化异常对象
            out.writeObject(th);
        }
        if (attach) {
            // 记录Dubbo协议版本
            result.getAttachments().put(DUBBO_VERSION_KEY, Version.getProtocolVersion());
            // 序列化attachments集合
            out.writeObject(result.getAttachments());
        }
    }

}
