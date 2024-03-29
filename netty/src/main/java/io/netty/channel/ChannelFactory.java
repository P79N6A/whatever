package io.netty.channel;

@SuppressWarnings({"ClassNameSameAsAncestorName", "deprecation"})
public interface ChannelFactory<T extends Channel> extends io.netty.bootstrap.ChannelFactory<T> {

    @Override
    T newChannel();
}
