package io.netty.channel.socket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

import java.net.InetAddress;
import java.net.NetworkInterface;

public interface DatagramChannelConfig extends ChannelConfig {

    int getSendBufferSize();

    DatagramChannelConfig setSendBufferSize(int sendBufferSize);

    int getReceiveBufferSize();

    DatagramChannelConfig setReceiveBufferSize(int receiveBufferSize);

    int getTrafficClass();

    DatagramChannelConfig setTrafficClass(int trafficClass);

    boolean isReuseAddress();

    DatagramChannelConfig setReuseAddress(boolean reuseAddress);

    boolean isBroadcast();

    DatagramChannelConfig setBroadcast(boolean broadcast);

    boolean isLoopbackModeDisabled();

    DatagramChannelConfig setLoopbackModeDisabled(boolean loopbackModeDisabled);

    int getTimeToLive();

    DatagramChannelConfig setTimeToLive(int ttl);

    InetAddress getInterface();

    DatagramChannelConfig setInterface(InetAddress interfaceAddress);

    NetworkInterface getNetworkInterface();

    DatagramChannelConfig setNetworkInterface(NetworkInterface networkInterface);

    @Override
    @Deprecated
    DatagramChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    @Override
    DatagramChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    DatagramChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    DatagramChannelConfig setAllocator(ByteBufAllocator allocator);

    @Override
    DatagramChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    @Override
    DatagramChannelConfig setAutoRead(boolean autoRead);

    @Override
    DatagramChannelConfig setAutoClose(boolean autoClose);

    @Override
    DatagramChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);

    @Override
    DatagramChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark);

}
