package io.netty.channel;

import io.netty.util.ReferenceCounted;

import java.net.SocketAddress;

public interface AddressedEnvelope<M, A extends SocketAddress> extends ReferenceCounted {

    M content();

    A sender();

    A recipient();

    @Override
    AddressedEnvelope<M, A> retain();

    @Override
    AddressedEnvelope<M, A> retain(int increment);

    @Override
    AddressedEnvelope<M, A> touch();

    @Override
    AddressedEnvelope<M, A> touch(Object hint);
}
