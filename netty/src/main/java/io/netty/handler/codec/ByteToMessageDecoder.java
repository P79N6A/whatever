package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.util.internal.StringUtil;

import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkPositive;

/**
 * 解码结果是消息帧，处理TCP粘包
 */
public abstract class ByteToMessageDecoder extends ChannelInboundHandlerAdapter {

    /*
     * 两个累积器
     */

    public static final Cumulator MERGE_CUMULATOR = new Cumulator() {

        /**
         * cumulation：已经累积的字节数据
         * in：该次channelRead读取到的数据
         * 返回ByteBuf为累积数据后的新累积区（必要时候自动扩容）
         */
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            try {
                final ByteBuf buffer;
                // 两种情况会扩容：
                // 1. 累积区容量不够容纳数据
                // 2. 用户使用了slice().retain()或duplicate().retain()使refCnt增加
                if (cumulation.writerIndex() > cumulation.maxCapacity() - in.readableBytes() || cumulation.refCnt() > 1 || cumulation.isReadOnly()) {
                    // 自动扩容
                    buffer = expandCumulation(alloc, cumulation, in.readableBytes());
                } else {
                    buffer = cumulation;
                }
                buffer.writeBytes(in);
                return buffer;
            } finally {
                in.release();
            }
        }
    };

    public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator() {
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            ByteBuf buffer;
            try {
                // 只在第二种情况refCnt>1时扩容
                if (cumulation.refCnt() > 1) {
                    buffer = expandCumulation(alloc, cumulation, in.readableBytes());
                    buffer.writeBytes(in);
                } else {
                    CompositeByteBuf composite;
                    if (cumulation instanceof CompositeByteBuf) {
                        composite = (CompositeByteBuf) cumulation;
                    } else {
                        // 当cumulation不是CompositeByteBuf时会创建新CompositeByteBuf
                        composite = alloc.compositeBuffer(Integer.MAX_VALUE);
                        composite.addComponent(true, cumulation);
                    }
                    // 当容量不够时不会内存复制，只会将新读入的in加到CompositeByteBuf中，要求用户维护索引，Netty默认MERGE_CUMULATOR
                    composite.addComponent(true, in);
                    in = null;
                    buffer = composite;
                }
                return buffer;
            } finally {
                if (in != null) {
                    in.release();
                }
            }
        }
    };

    private static final byte STATE_INIT = 0;
    private static final byte STATE_CALLING_CHILD_DECODE = 1;
    private static final byte STATE_HANDLER_REMOVED_PENDING = 2;

    /**
     * 累积区
     */
    ByteBuf cumulation;

    /**
     * 累积器
     */
    private Cumulator cumulator = MERGE_CUMULATOR;

    /**
     * 设置为true后每个channelRead事件只解码出一个结果
     */
    private boolean singleDecode;

    /**
     * 解码结果为空
     */
    private boolean decodeWasNull;

    /**
     * 是否首个消息
     */
    private boolean first;

    private byte decodeState = STATE_INIT;

    /**
     * 累积区不丢弃字节的最大次数，16次后开始丢弃
     */
    private int discardAfterReads = 16;

    /**
     * 累积区不丢弃字节的channelRead次数
     */
    private int numReads;

    /**
     * 构造方法
     */
    protected ByteToMessageDecoder() {
        ensureNotSharable();
    }

    static void fireChannelRead(ChannelHandlerContext ctx, List<Object> msgs, int numElements) {
        if (msgs instanceof CodecOutputList) {
            fireChannelRead(ctx, (CodecOutputList) msgs, numElements);
        } else {
            for (int i = 0; i < numElements; i++) {
                ctx.fireChannelRead(msgs.get(i));
            }
        }
    }

    static void fireChannelRead(ChannelHandlerContext ctx, CodecOutputList msgs, int numElements) {
        for (int i = 0; i < numElements; i++) {
            ctx.fireChannelRead(msgs.getUnsafe(i));
        }
    }

    static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable) {
        ByteBuf oldCumulation = cumulation;

        // 扩容后新的缓冲区
        cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);

        // 扩容，直接用大容量的Bytebuf替换旧的ByteBuf
        cumulation.writeBytes(oldCumulation);

        // 旧的缓冲区释放
        oldCumulation.release();
        return cumulation;
    }

    public boolean isSingleDecode() {
        return singleDecode;
    }

    public void setSingleDecode(boolean singleDecode) {
        this.singleDecode = singleDecode;
    }

    public void setCumulator(Cumulator cumulator) {
        if (cumulator == null) {
            throw new NullPointerException("cumulator");
        }
        this.cumulator = cumulator;
    }

    public void setDiscardAfterReads(int discardAfterReads) {
        checkPositive(discardAfterReads, "discardAfterReads");
        this.discardAfterReads = discardAfterReads;
    }

    protected int actualReadableBytes() {
        return internalBuffer().readableBytes();
    }

    protected ByteBuf internalBuffer() {
        if (cumulation != null) {
            return cumulation;
        } else {
            return Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public final void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (decodeState == STATE_CALLING_CHILD_DECODE) {
            decodeState = STATE_HANDLER_REMOVED_PENDING;
            return;
        }
        ByteBuf buf = cumulation;
        if (buf != null) {
            // 释放累积区，GC
            cumulation = null;

            int readable = buf.readableBytes();
            if (readable > 0) {
                ByteBuf bytes = buf.readBytes(readable);
                buf.release();
                // 当解码器被删除时，如果还有没被解码的数据，则将数据传播到下一个Handler
                ctx.fireChannelRead(bytes);
            } else {
                buf.release();
            }
            // 置0，有可能被再次添加
            numReads = 0;
            ctx.fireChannelReadComplete();
        }
        // 自定义处理
        handlerRemoved0(ctx);
    }

    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
    }

    /**
     * 1. 累加新读取的数据到本地自己容器中
     * 2. 将本地字节容器中的数据传递给业务拆包器拆包
     * 3. 清理字节容器
     * 4. 传递业务数据包给业务解码器处理
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 只对ByteBuf处理
        if (msg instanceof ByteBuf) {
            // 解码结果列表，在线程中被缓存，可循环使用来存储解码结果
            CodecOutputList out = CodecOutputList.newInstance();
            try {
                ByteBuf data = (ByteBuf) msg;
                // 累积区为空表示首次解码
                first = cumulation == null;
                if (first) {
                    // 首次解码直接使用读入的ByteBuf作为累积区
                    cumulation = data;
                } else {
                    // 非首次需要进行字节数据累积
                    cumulation = cumulator.cumulate(ctx.alloc(), cumulation, data);
                }
                // 解码操作
                callDecode(ctx, cumulation, out);
            } catch (DecoderException e) {
                throw e;
            } catch (Exception e) {
                throw new DecoderException(e);
            } finally {
                if (cumulation != null && !cumulation.isReadable()) {
                    // 此时累积区不再有字节数据，已被处理完毕
                    numReads = 0;
                    // 一条消息被解码完毕后，如果客户端长时间不发送消息，服务端保存该条消息的累积区将一直占据内存，必须释放该累积区
                    cumulation.release();
                    cumulation = null;
                }
                // 累积区的数据一直在channelRead读取数据进行累积和解码，直到达到了discardAfterReads次，此时累积区依然还有数据
                else if (++numReads >= discardAfterReads) {
                    numReads = 0;
                    // 主动丢弃一些字节，防止该累积区占用大量内存
                    discardSomeReadBytes();
                }

                int size = out.size();
                // 本次没有解码出数据，此时size=0
                decodeWasNull = !out.insertSinceRecycled();

                // 统一触发ChannelRead事件，将解码出的数据传递给下一个处理器
                fireChannelRead(ctx, out, size);
                // 回收解码结果
                out.recycle();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 连续读次数置0
        numReads = 0;
        // 丢弃已读数据，节约内存
        discardSomeReadBytes();
        if (decodeWasNull) {
            decodeWasNull = false;
            if (!ctx.channel().config().isAutoRead()) {
                // 如果channelRead()中没有解码出消息，可能是数据不够，调用ctx.read()期待读入更多的数据
                ctx.read();
            }
        }
        ctx.fireChannelReadComplete();
    }

    protected final void discardSomeReadBytes() {
        // 累积区的refCnt() == 1时才丢弃数据
        // 如果用户使用了slice().retain()和duplicate().retain()使refCnt>1，
        // 表明该累积区还在被用户使用，须确定用户不再使用该累积区的已读数据
        if (cumulation != null && !first && cumulation.refCnt() == 1) {
            cumulation.discardSomeReadBytes();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelInputClosed(ctx, true);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ChannelInputShutdownEvent) {

            channelInputClosed(ctx, false);
        }
        super.userEventTriggered(ctx, evt);
    }

    private void channelInputClosed(ChannelHandlerContext ctx, boolean callChannelInactive) throws Exception {
        CodecOutputList out = CodecOutputList.newInstance();
        try {
            channelInputClosed(ctx, out);
        } catch (DecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new DecoderException(e);
        } finally {
            try {
                if (cumulation != null) {
                    cumulation.release();
                    cumulation = null;
                }
                int size = out.size();
                fireChannelRead(ctx, out, size);
                if (size > 0) {

                    ctx.fireChannelReadComplete();
                }
                if (callChannelInactive) {
                    ctx.fireChannelInactive();
                }
            } finally {

                out.recycle();
            }
        }
    }

    void channelInputClosed(ChannelHandlerContext ctx, List<Object> out) throws Exception {
        if (cumulation != null) {
            callDecode(ctx, cumulation, out);
            decodeLast(ctx, cumulation, out);
        } else {
            decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
        }
    }

    protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            while (in.isReadable()) {
                int outSize = out.size();

                if (outSize > 0) {
                    // 解码出消息就立即处理，防止消息等待
                    fireChannelRead(ctx, out, outSize);
                    out.clear();

                    // 调用decode()方法的前后，都检查该Handler是否被用户从ChannelPipeline中删除，如果删除则跳出解码步骤不对输入缓冲区in进行操作
                    if (ctx.isRemoved()) {
                        break;
                    }
                    outSize = 0;
                }

                int oldInputLength = in.readableBytes();
                // 子类需要实现的具体解码步骤
                decodeRemovalReentryProtection(ctx, in, out);

                // 用户主动删除该Handler，继续操作in是不安全的
                if (ctx.isRemoved()) {
                    break;
                }
                // 此时outSize都==0
                if (outSize == out.size()) {
                    if (oldInputLength == in.readableBytes()) {
                        // 没有解码出消息，且没读取任何in数据
                        break;
                    } else {
                        // 读取了一部份数据但没有解码出消息，说明需要更多的数据，故继续
                        continue;
                    }
                }
                // 运行到这里outSize>0 说明已经解码出消息

                // 解码完成后，对in解码前后的读索引进行检查
                if (oldInputLength == in.readableBytes()) {
                    // 解码出消息但是in的读索引不变，decode方法有Bug
                    throw new DecoderException(StringUtil.simpleClassName(getClass()) + ".decode() did not read anything but decoded a message.");
                }
                // 用户设定一个channelRead事件只解码一次
                if (isSingleDecode()) {
                    break;
                }
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception cause) {
            throw new DecoderException(cause);
        }
    }

    /**
     * in：累积器已累积的数据，out表示本次可从累积数据解码出的结果列表，结果可为POJO对象或ByteBuf
     */
    protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;

    final void decodeRemovalReentryProtection(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        decodeState = STATE_CALLING_CHILD_DECODE;
        try {
            decode(ctx, in, out);
        } finally {
            boolean removePending = decodeState == STATE_HANDLER_REMOVED_PENDING;
            decodeState = STATE_INIT;
            if (removePending) {
                handlerRemoved(ctx);
            }
        }
    }

    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.isReadable()) {
            decodeRemovalReentryProtection(ctx, in, out);
        }
    }

    public interface Cumulator {
        ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in);
    }
}
