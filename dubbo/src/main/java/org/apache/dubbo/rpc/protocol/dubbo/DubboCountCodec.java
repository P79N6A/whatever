package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.MultiMessage;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;

import java.io.IOException;

import static org.apache.dubbo.common.constants.RpcConstants.INPUT_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.OUTPUT_KEY;

public final class DubboCountCodec implements Codec2 {

    private DubboCodec codec = new DubboCodec();

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object msg) throws IOException {
        codec.encode(channel, buffer, msg);
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        // 读索引
        int save = buffer.readerIndex();
        // 解码结果列表
        MultiMessage result = MultiMessage.create();
        // 循环解码
        do {
            // 解码结果
            Object obj = codec.decode(channel, buffer);
            // 等待需要更多输入
            if (Codec2.DecodeResult.NEED_MORE_INPUT == obj) {
                // 标记读索引
                buffer.readerIndex(save);
                // 跳出
                break;
            } else {
                // 加入列表
                result.addMessage(obj);
                // 结果长度：当前读索引-之前
                logMessageLength(obj, buffer.readerIndex() - save);
                // 当前读索引
                save = buffer.readerIndex();
            }
        } while (true);
        if (result.isEmpty()) {
            return Codec2.DecodeResult.NEED_MORE_INPUT;
        }
        if (result.size() == 1) {
            return result.get(0);
        }
        return result;
    }

    private void logMessageLength(Object result, int bytes) {
        if (bytes <= 0) {
            return;
        }
        if (result instanceof Request) {
            try {
                ((RpcInvocation) ((Request) result).getData()).setAttachment(INPUT_KEY, String.valueOf(bytes));
            } catch (Throwable e) {
            }
        } else if (result instanceof Response) {
            try {
                ((RpcResult) ((Response) result).getResult()).setAttachment(OUTPUT_KEY, String.valueOf(bytes));
            } catch (Throwable e) {
            }
        }
    }

}
