package io.netty.channel.socket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

public interface ServerSocketChannelConfig extends ChannelConfig {

    int getBacklog();

    ServerSocketChannelConfig setBacklog(int backlog);

    boolean isReuseAddress();

    ServerSocketChannelConfig setReuseAddress(boolean reuseAddress);

    int getReceiveBufferSize();

    ServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

    ServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

    @Override
    ServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    @Deprecated
    ServerSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    @Override
    ServerSocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    ServerSocketChannelConfig setAllocator(ByteBufAllocator allocator);

    @Override
    ServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    @Override
    ServerSocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    ServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);

    @Override
    ServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);

    @Override
    ServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);

    @Override
    ServerSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark);

}
