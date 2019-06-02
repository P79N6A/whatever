package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AttributeMap;

import java.net.SocketAddress;

/**
 * 正常结束的Channel状态转移
 * REGISTERED -> CONNECT/BIND -> ACTIVE -> CLOSE- > INACTIVE -> UNREGISTERED
 * REGISTERED -> ACTIVE -> CLOSE -> INACTIVE -> UNREGISTERED
 * 其中第一种是服务端用于绑定的Channel或者客户端用于发起连接的Channel，第二种是服务端接受的SocketChannel
 */
public interface Channel extends AttributeMap, ChannelOutboundInvoker, Comparable<Channel> {

    ChannelId id();

    EventLoop eventLoop();

    /**
     * 一个Channel由另一个Channel创建，则形成父子关系
     * 当ServerSocketChannel通过accept一个SocketChannel时，SocketChannel的父亲是ServerSocketChannel
     */
    Channel parent();

    ChannelConfig config();

    boolean isOpen();

    /**
     * 是否注册到一个EventLoop
     */
    boolean isRegistered();

    /**
     * 是否激活
     * 对于ServerSocketChannel，表示Channel已绑定到端口
     * 对于SocketChannel，表示Channel可用且已连接到对端
     */

    boolean isActive();

    ChannelMetadata metadata();

    SocketAddress localAddress();

    SocketAddress remoteAddress();

    ChannelFuture closeFuture();

    /**
     * Writable表示Channel的可写状态，当Channel的写缓冲区outboundBuffer非null且可写时返回true
     */
    boolean isWritable();

    long bytesBeforeUnwritable();

    long bytesBeforeWritable();

    Unsafe unsafe();

    ChannelPipeline pipeline();

    ByteBufAllocator alloc();

    @Override
    Channel read();

    @Override
    Channel flush();

    /**
     * 这里的I/O事件都是outbound，表示由用户发起，对应地，inbound，将在ChannelPipeline
     */
    interface Unsafe {

        RecvByteBufAllocator.Handle recvBufAllocHandle();

        SocketAddress localAddress();

        SocketAddress remoteAddress();

        void register(EventLoop eventLoop, ChannelPromise promise);

        void bind(SocketAddress localAddress, ChannelPromise promise);

        void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

        void disconnect(ChannelPromise promise);

        void close(ChannelPromise promise);

        void closeForcibly();

        void deregister(ChannelPromise promise);

        void beginRead();

        void write(Object msg, ChannelPromise promise);

        void flush();

        ChannelPromise voidPromise();

        /**
         * 写缓冲区
         */
        ChannelOutboundBuffer outboundBuffer();
    }
}
