package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.UncheckedBooleanSupplier;
import io.netty.util.internal.UnstableApi;

public interface RecvByteBufAllocator {

    Handle newHandle();

    @Deprecated
    interface Handle {

        ByteBuf allocate(ByteBufAllocator alloc);

        int guess();

        void reset(ChannelConfig config);

        void incMessagesRead(int numMessages);

        void lastBytesRead(int bytes);

        int lastBytesRead();

        void attemptedBytesRead(int bytes);

        int attemptedBytesRead();

        boolean continueReading();

        void readComplete();
    }

    @SuppressWarnings("deprecation")
    @UnstableApi
    interface ExtendedHandle extends Handle {

        boolean continueReading(UncheckedBooleanSupplier maybeMoreDataSupplier);
    }

}
