package io.netty.channel.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

public interface DatagramChannel extends Channel {
    @Override
    DatagramChannelConfig config();

    @Override
    InetSocketAddress localAddress();

    @Override
    InetSocketAddress remoteAddress();

    boolean isConnected();

    ChannelFuture joinGroup(InetAddress multicastAddress);

    ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise future);

    ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface);

    ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise future);

    ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source);

    ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise future);

    ChannelFuture leaveGroup(InetAddress multicastAddress);

    ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise future);

    ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface);

    ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise future);

    ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source);

    ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise future);

    ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock);

    ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock, ChannelPromise future);

    ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock);

    ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock, ChannelPromise future);
}
