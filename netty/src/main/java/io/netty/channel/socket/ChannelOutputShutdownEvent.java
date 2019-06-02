package io.netty.channel.socket;

import io.netty.util.internal.UnstableApi;

@UnstableApi
public final class ChannelOutputShutdownEvent {
    public static final ChannelOutputShutdownEvent INSTANCE = new ChannelOutputShutdownEvent();

    private ChannelOutputShutdownEvent() {
    }
}
