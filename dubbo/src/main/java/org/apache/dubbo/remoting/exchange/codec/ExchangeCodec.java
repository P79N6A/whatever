package org.apache.dubbo.remoting.exchange.codec;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBufferInputStream;
import org.apache.dubbo.remoting.buffer.ChannelBufferOutputStream;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.remoting.telnet.codec.TelnetCodec;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.remoting.transport.ExceedPayloadLimitException;

import java.io.IOException;
import java.io.InputStream;

/*
 * 偏移量(Bit)	字段	取值
 * 0 ~ 7 header[0]	魔数高位
 *      0xda00
 * 8 ~ 15 header[1]	魔数低位
 *      0xbb
 * 16 header[2]	数据包类型
 *      0 - Response
 *      1 - Request
 * 17	调用方式	仅在第16位被设为1的情况下有效
 *      0 - 单向调用
 *      1 - 双向调用
 * 18	事件标识
 *      0 - 当前数据包是请求或响应包
 *      1 - 当前数据包是心跳包
 * 19 ~ 23	序列化器编号
 *      2 - Hessian2Serialization
 *      3 - JavaSerialization
 *      4 - CompactedJavaSerialization
 *      6 - FastJsonSerialization
 *      7 - NativeJavaSerialization
 *      8 - KryoSerialization
 *      9 - FstSerialization
 * 24 ~ 31 header[3]	状态
 *      20 - OK
 *      30 - CLIENT_TIMEOUT
 *      31 - SERVER_TIMEOUT
 *      40 - BAD_REQUEST
 *      50 - BAD_RESPONSE
 *      ......
 * 32 ~ 95 header[4]	请求编号
 *      共8字节，运行时生成
 * 96 ~ 127 header[12]	消息体长度
 *      运行时计算
 */
public class ExchangeCodec extends TelnetCodec {
    /**
     * 消息头长度
     */
    protected static final int HEADER_LENGTH = 16;

    /**
     * 魔数内容
     */
    protected static final short MAGIC = (short) 0xdabb;

    protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];

    protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];

    protected static final byte FLAG_REQUEST = (byte) 0x80;

    protected static final byte FLAG_TWOWAY = (byte) 0x40;

    protected static final byte FLAG_EVENT = (byte) 0x20;

    protected static final int SERIALIZATION_MASK = 0x1f;

    private static final Logger logger = LoggerFactory.getLogger(ExchangeCodec.class);

    public Short getMagicCode() {
        return MAGIC;
    }

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object msg) throws IOException {
        if (msg instanceof Request) {
            // 对Request对象编码
            encodeRequest(channel, buffer, (Request) msg);
        } else if (msg instanceof Response) {
            // 对Response对象编码
            encodeResponse(channel, buffer, (Response) msg);
        } else {
            super.encode(channel, buffer, msg);
        }
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        // 可读长度
        int readable = buffer.readableBytes();
        // 创建消息头字节数组
        byte[] header = new byte[Math.min(readable, HEADER_LENGTH)];
        // 读取消息头数据
        buffer.readBytes(header);
        // 调用重载方法进行后续解码
        return decode(channel, buffer, readable, header);
    }

    @Override
    protected Object decode(Channel channel, ChannelBuffer buffer, int readable, byte[] header) throws IOException {
        // 可读长度>0 && 消息头第一位不等于魔数高位 || 可读长度>1 && 消息头第二位不等于魔数低位
        if (readable > 0 && header[0] != MAGIC_HIGH || readable > 1 && header[1] != MAGIC_LOW) {
            int length = header.length;
            if (header.length < readable) {
                header = Bytes.copyOf(header, readable);
                buffer.readBytes(header, length, readable - length);
            }
            for (int i = 1; i < header.length - 1; i++) {
                if (header[i] == MAGIC_HIGH && header[i + 1] == MAGIC_LOW) {
                    buffer.readerIndex(buffer.readerIndex() - header.length + i);
                    header = Bytes.copyOf(header, i);
                    break;
                }
            }
            // 通过telnet命令行发送的数据包不包含消息头，调用TelnetCodec的decode方法对数据包进行解码
            return super.decode(channel, buffer, readable, header);
        }
        // 可读长度<消息头长度
        if (readable < HEADER_LENGTH) {
            // 返回DecodeResult.NEED_MORE_INPUT
            return DecodeResult.NEED_MORE_INPUT;
        }
        // 从消息头中获取消息体长度
        int len = Bytes.bytes2int(header, 12);
        // 检测消息体长度是否超出限制，超出则抛出异常
        checkPayload(channel, len);
        // 消息体长度+消息头长度
        int tt = len + HEADER_LENGTH;
        // 检测可读的字节数是否小于实际的字节数
        if (readable < tt) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        ChannelBufferInputStream is = new ChannelBufferInputStream(buffer, len);
        try {
            // 解码消息体
            return decodeBody(channel, is, header);
        } finally {
            if (is.available() > 0) {
                try {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Skip input stream " + is.available());
                    }
                    StreamUtils.skipUnusedStream(is);
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 注释都写完了才发觉这个方法在DubboCodec被重写了，，，基本上没什么区别
     */
    protected Object decodeBody(Channel channel, InputStream is, byte[] header) throws IOException {
        // 消息标识，消息头第3字节（24~31）
        byte flag = header[2],
                // 序列化器编号
                proto = (byte) (flag & SERIALIZATION_MASK);
        // 消息id，消息头第5字节（32~95）
        long id = Bytes.bytes2long(header, 4);
        // 响应消息
        if ((flag & FLAG_REQUEST) == 0) {
            // 生成对应的响应
            Response res = new Response(id);
            // 事件类型
            if ((flag & FLAG_EVENT) != 0) {
                res.setEvent(true);
            }
            // 状态
            byte status = header[3];
            res.setStatus(status);
            try {
                // 获取反序列化扩展
                ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                // 状态OK
                if (status == Response.OK) {
                    Object data;
                    // 心跳响应
                    if (res.isHeartbeat()) {
                        data = decodeHeartbeatData(channel, in);
                    }
                    // 事件响应
                    else if (res.isEvent()) {
                        data = decodeEventData(channel, in);
                    }
                    //
                    else {
                        data = decodeResponseData(channel, in, getRequestData(id));
                    }
                    // 设置数据
                    res.setResult(data);
                } else {
                    res.setErrorMessage(in.readUTF());
                }
            } catch (Throwable t) {
                res.setStatus(Response.CLIENT_ERROR);
                res.setErrorMessage(StringUtils.toString(t));
            }
            return res;
        }
        // 请求消息
        else {
            Request req = new Request(id);
            // 协议版本
            req.setVersion(Version.getProtocolVersion());
            // 调用方式
            req.setTwoWay((flag & FLAG_TWOWAY) != 0);
            if ((flag & FLAG_EVENT) != 0) {
                req.setEvent(true);
            }
            try {
                ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                Object data;
                if (req.isHeartbeat()) {
                    data = decodeHeartbeatData(channel, in);
                } else if (req.isEvent()) {
                    data = decodeEventData(channel, in);
                } else {
                    data = decodeRequestData(channel, in);
                }
                req.setData(data);
            } catch (Throwable t) {
                req.setBroken(true);
                req.setData(t);
            }
            return req;
        }
    }

    protected Object getRequestData(long id) {
        DefaultFuture future = DefaultFuture.getFuture(id);
        if (future == null) {
            return null;
        }
        Request req = future.getRequest();
        if (req == null) {
            return null;
        }
        return req.getData();
    }

    protected void encodeRequest(Channel channel, ChannelBuffer buffer, Request req) throws IOException {
        Serialization serialization = getSerialization(channel);
        // 创建消息头字节数组，长度为16
        byte[] header = new byte[HEADER_LENGTH];
        // 设置魔数
        Bytes.short2bytes(MAGIC, header);
        // 设置数据包类型（Request/Response）和序列化器编号
        header[2] = (byte) (FLAG_REQUEST | serialization.getContentTypeId());
        // 设置通信方式(单向/双向)
        if (req.isTwoWay()) {
            header[2] |= FLAG_TWOWAY;
        }
        // 设置事件标识
        if (req.isEvent()) {
            header[2] |= FLAG_EVENT;
        }
        // 设置请求编号，8个字节，从第4个字节开始设置
        Bytes.long2bytes(req.getId(), header, 4);
        // 获取buffer当前的写位置
        int savedWriteIndex = buffer.writerIndex();
        // 更新writerIndex，为消息头预留16个字节的空间
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
        ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
        // 创建序列化器，比如Hessian2ObjectOutput
        ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
        if (req.isEvent()) {
            // 对事件数据进行序列化操作
            encodeEventData(channel, out, req.getData());
        } else {
            // 对请求数据进行序列化操作
            encodeRequestData(channel, out, req.getData(), req.getVersion());
        }
        out.flushBuffer();
        if (out instanceof Cleanable) {
            ((Cleanable) out).cleanup();
        }
        bos.flush();
        bos.close();
        // 获取写入的字节数，也就是消息体长度
        int len = bos.writtenBytes();
        checkPayload(channel, len);
        // 将消息体长度写入到消息头中
        Bytes.int2bytes(len, header, 12);
        // 将buffer指针移动到savedWriteIndex，为写消息头做准备
        buffer.writerIndex(savedWriteIndex);
        // 从savedWriteIndex 下标处写入消息头
        buffer.writeBytes(header);
        // 设置新的writerIndex，writerIndex = 原写下标 + 消息头长度 + 消息体长度
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
    }

    protected void encodeResponse(Channel channel, ChannelBuffer buffer, Response res) throws IOException {
        int savedWriteIndex = buffer.writerIndex();
        try {
            // SPI扩展，url中没指定的话默认hessian2
            Serialization serialization = getSerialization(channel);
            // 消息头数组，16字节
            byte[] header = new byte[HEADER_LENGTH];
            // 设置魔数0xdabb
            Bytes.short2bytes(MAGIC, header);
            // 序列化方式
            header[2] = serialization.getContentTypeId();
            // 心跳消息还是正常消息
            if (res.isHeartbeat()) {
                header[2] |= FLAG_EVENT;
            }
            // 响应状态
            byte status = res.getStatus();
            // 设置响应状态
            header[3] = status;
            // 设置请求id
            Bytes.long2bytes(res.getId(), header, 4);
            // 更新writerIndex，为消息头预留16个字节的空间
            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
            ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
            ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
            if (status == Response.OK) {
                if (res.isHeartbeat()) {
                    // 对心跳响应结果进行序列化，已废弃
                    encodeHeartbeatData(channel, out, res.getResult());
                } else {
                    // 对调用结果进行序列化
                    encodeResponseData(channel, out, res.getResult(), res.getVersion());
                }
            } else {
                // 对错误信息进行序列化
                out.writeUTF(res.getErrorMessage());
            }
            out.flushBuffer();
            if (out instanceof Cleanable) {
                ((Cleanable) out).cleanup();
            }
            bos.flush();
            bos.close();
            // 获取写入的字节数，也就是消息体长度
            int len = bos.writtenBytes();
            checkPayload(channel, len);
            // 将消息体长度写入到消息头中
            Bytes.int2bytes(len, header, 12);
            // 将buffer指针移动到savedWriteIndex，为写消息头做准备
            buffer.writerIndex(savedWriteIndex);
            // 从savedWriteIndex下标处写入消息头
            buffer.writeBytes(header);
            // 设置新的writerIndex，writerIndex = 原写下标 + 消息头长度 + 消息体长度
            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
        } catch (Throwable t) {
            // 异常处理逻辑
            buffer.writerIndex(savedWriteIndex);
            if (!res.isEvent() && res.getStatus() != Response.BAD_RESPONSE) {
                Response r = new Response(res.getId(), res.getVersion());
                r.setStatus(Response.BAD_RESPONSE);
                if (t instanceof ExceedPayloadLimitException) {
                    logger.warn(t.getMessage(), t);
                    try {
                        r.setErrorMessage(t.getMessage());
                        channel.send(r);
                        return;
                    } catch (RemotingException e) {
                        logger.warn("Failed to send bad_response info back: " + t.getMessage() + ", cause: " + e.getMessage(), e);
                    }
                } else {
                    logger.warn("Fail to encode response: " + res + ", send bad_response info instead, cause: " + t.getMessage(), t);
                    try {
                        r.setErrorMessage("Failed to send response: " + res + ", cause: " + StringUtils.toString(t));
                        channel.send(r);
                        return;
                    } catch (RemotingException e) {
                        logger.warn("Failed to send bad_response info back: " + res + ", cause: " + e.getMessage(), e);
                    }
                }
            }
            if (t instanceof IOException) {
                throw (IOException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new RuntimeException(t.getMessage(), t);
            }
        }
    }

    @Override
    protected Object decodeData(ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    @Deprecated
    protected Object decodeHeartbeatData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeResponseData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    @Override
    protected void encodeData(ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    private void encodeEventData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    @Deprecated
    protected void encodeHeartbeatData(ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    protected void encodeRequestData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    protected void encodeResponseData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    @Override
    protected Object decodeData(Channel channel, ObjectInput in) throws IOException {
        return decodeRequestData(channel, in);
    }

    protected Object decodeEventData(Channel channel, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    @Deprecated
    protected Object decodeHeartbeatData(Channel channel, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(Channel channel, ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    protected Object decodeResponseData(Channel channel, ObjectInput in) throws IOException {
        return decodeResponseData(in);
    }

    protected Object decodeResponseData(Channel channel, ObjectInput in, Object requestData) throws IOException {
        return decodeResponseData(channel, in);
    }

    @Override
    protected void encodeData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(channel, out, data);
    }

    private void encodeEventData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    @Deprecated
    protected void encodeHeartbeatData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeHeartbeatData(out, data);
    }

    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeResponseData(out, data);
    }

    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data, String version) throws IOException {
        encodeRequestData(out, data);
    }

    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data, String version) throws IOException {
        encodeResponseData(out, data);
    }

}
