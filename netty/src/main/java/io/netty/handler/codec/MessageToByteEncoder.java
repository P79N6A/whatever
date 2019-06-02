package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

/**
 * POJO对象编码为字节数据存储到ByteBuf，只需定义编码方法encode()，只处理出站事件（write）
 */
public abstract class MessageToByteEncoder<I> extends ChannelOutboundHandlerAdapter {

    /**
     * 检测泛型参数是否是期待的类型
     */
    private final TypeParameterMatcher matcher;

    /**
     * 是否使用DirectedByteBuf，默认true
     */
    private final boolean preferDirect;

    /**
     * 构造方法
     */
    protected MessageToByteEncoder() {
        this(true);
    }

    protected MessageToByteEncoder(Class<? extends I> outboundMessageType) {
        this(outboundMessageType, true);
    }

    protected MessageToByteEncoder(boolean preferDirect) {
        matcher = TypeParameterMatcher.find(this, MessageToByteEncoder.class, "I");
        this.preferDirect = preferDirect;
    }

    protected MessageToByteEncoder(Class<? extends I> outboundMessageType, boolean preferDirect) {
        matcher = TypeParameterMatcher.get(outboundMessageType);
        this.preferDirect = preferDirect;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = null;
        try {
            // 检测类型是否匹配
            if (acceptOutboundMessage(msg)) {
                @SuppressWarnings("unchecked") I cast = (I) msg;
                // 分配一个输出缓冲区
                buf = allocateBuffer(ctx, cast, preferDirect);
                try {
                    // 编码
                    encode(ctx, cast, buf);
                } finally {
                    ReferenceCountUtil.release(cast);
                }
                // ByteBuf可读
                if (buf.isReadable()) {
                    // 写入
                    ctx.write(buf, promise);
                } else {
                    buf.release();
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                buf = null;
            } else {
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }

    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, @SuppressWarnings("unused") I msg, boolean preferDirect) throws Exception {
        if (preferDirect) {
            // 内核直接缓存
            return ctx.alloc().ioBuffer();
        } else {
            // JAVA堆缓存
            return ctx.alloc().heapBuffer();
        }
    }

    protected abstract void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception;

    protected boolean isPreferDirect() {
        return preferDirect;
    }
}
