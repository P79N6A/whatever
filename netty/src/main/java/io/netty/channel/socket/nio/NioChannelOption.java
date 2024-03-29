package io.netty.channel.socket.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class NioChannelOption<T> extends ChannelOption<T> {

    private final java.net.SocketOption<T> option;

    @SuppressWarnings("deprecation")
    private NioChannelOption(java.net.SocketOption<T> option) {
        super(option.name());
        this.option = option;
    }

    public static <T> ChannelOption<T> of(java.net.SocketOption<T> option) {
        return new NioChannelOption<T>(option);
    }

    static <T> boolean setOption(Channel jdkChannel, NioChannelOption<T> option, T value) {
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;
        if (!channel.supportedOptions().contains(option.option)) {
            return false;
        }
        if (channel instanceof ServerSocketChannel && option.option == java.net.StandardSocketOptions.IP_TOS) {

            return false;
        }
        try {
            channel.setOption(option.option, value);
            return true;
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    static <T> T getOption(Channel jdkChannel, NioChannelOption<T> option) {
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;

        if (!channel.supportedOptions().contains(option.option)) {
            return null;
        }
        if (channel instanceof ServerSocketChannel && option.option == java.net.StandardSocketOptions.IP_TOS) {

            return null;
        }
        try {
            return channel.getOption(option.option);
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static ChannelOption[] getOptions(Channel jdkChannel) {
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;
        Set<java.net.SocketOption<?>> supportedOpts = channel.supportedOptions();

        if (channel instanceof ServerSocketChannel) {
            List<ChannelOption<?>> extraOpts = new ArrayList<ChannelOption<?>>(supportedOpts.size());
            for (java.net.SocketOption<?> opt : supportedOpts) {
                if (opt == java.net.StandardSocketOptions.IP_TOS) {

                    continue;
                }
                extraOpts.add(new NioChannelOption(opt));
            }
            return extraOpts.toArray(new ChannelOption[0]);
        } else {
            ChannelOption<?>[] extraOpts = new ChannelOption[supportedOpts.size()];

            int i = 0;
            for (java.net.SocketOption<?> opt : supportedOpts) {
                extraOpts[i++] = new NioChannelOption(opt);
            }
            return extraOpts;
        }
    }
}
