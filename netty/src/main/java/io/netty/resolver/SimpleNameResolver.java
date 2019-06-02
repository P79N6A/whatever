package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.UnstableApi;

import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

@UnstableApi
public abstract class SimpleNameResolver<T> implements NameResolver<T> {

    private final EventExecutor executor;

    protected SimpleNameResolver(EventExecutor executor) {
        this.executor = checkNotNull(executor, "executor");
    }

    protected EventExecutor executor() {
        return executor;
    }

    @Override
    public final Future<T> resolve(String inetHost) {
        final Promise<T> promise = executor().newPromise();
        return resolve(inetHost, promise);
    }

    @Override
    public Future<T> resolve(String inetHost, Promise<T> promise) {
        checkNotNull(promise, "promise");

        try {
            doResolve(inetHost, promise);
            return promise;
        } catch (Exception e) {
            return promise.setFailure(e);
        }
    }

    @Override
    public final Future<List<T>> resolveAll(String inetHost) {
        final Promise<List<T>> promise = executor().newPromise();
        return resolveAll(inetHost, promise);
    }

    @Override
    public Future<List<T>> resolveAll(String inetHost, Promise<List<T>> promise) {
        checkNotNull(promise, "promise");

        try {
            doResolveAll(inetHost, promise);
            return promise;
        } catch (Exception e) {
            return promise.setFailure(e);
        }
    }

    protected abstract void doResolve(String inetHost, Promise<T> promise) throws Exception;

    protected abstract void doResolveAll(String inetHost, Promise<List<T>> promise) throws Exception;

    @Override
    public void close() {
    }
}
