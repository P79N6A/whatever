package io.netty.resolver;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.UnstableApi;

import java.io.Closeable;
import java.util.List;

@UnstableApi
public interface NameResolver<T> extends Closeable {

    Future<T> resolve(String inetHost);

    Future<T> resolve(String inetHost, Promise<T> promise);

    Future<List<T>> resolveAll(String inetHost);

    Future<List<T>> resolveAll(String inetHost, Promise<List<T>> promise);

    @Override
    void close();
}
