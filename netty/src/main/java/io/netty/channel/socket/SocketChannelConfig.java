package io.netty.channel.socket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

public interface SocketChannelConfig extends ChannelConfig {

    boolean isTcpNoDelay();

    SocketChannelConfig setTcpNoDelay(boolean tcpNoDelay);

    int getSoLinger();

    SocketChannelConfig setSoLinger(int soLinger);

    int getSendBufferSize();

    SocketChannelConfig setSendBufferSize(int sendBufferSize);

    int getReceiveBufferSize();

    SocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

    boolean isKeepAlive();

    SocketChannelConfig setKeepAlive(boolean keepAlive);

    int getTrafficClass();

    SocketChannelConfig setTrafficClass(int trafficClass);

    boolean isReuseAddress();

    SocketChannelConfig setReuseAddress(boolean reuseAddress);

    SocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

    boolean isAllowHalfClosure();

    SocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure);

    @Override
    SocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    @Deprecated
    SocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    @Override
    SocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    SocketChannelConfig setAllocator(ByteBufAllocator allocator);

    @Override
    SocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    @Override
    SocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    SocketChannelConfig setAutoClose(boolean autoClose);

    @Override
    SocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);

    @Override
    SocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark);

}
