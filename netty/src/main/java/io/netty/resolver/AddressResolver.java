package io.netty.resolver;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.UnstableApi;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.List;

@UnstableApi
public interface AddressResolver<T extends SocketAddress> extends Closeable {

    boolean isSupported(SocketAddress address);

    boolean isResolved(SocketAddress address);

    Future<T> resolve(SocketAddress address);

    Future<T> resolve(SocketAddress address, Promise<T> promise);

    Future<List<T>> resolveAll(SocketAddress address);

    Future<List<T>> resolveAll(SocketAddress address, Promise<List<T>> promise);

    @Override
    void close();
}
