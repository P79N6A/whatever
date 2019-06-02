package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.TypeParameterMatcher;
import io.netty.util.internal.UnstableApi;

import java.net.SocketAddress;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Collections;
import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

@UnstableApi
public abstract class AbstractAddressResolver<T extends SocketAddress> implements AddressResolver<T> {

    private final EventExecutor executor;
    private final TypeParameterMatcher matcher;

    protected AbstractAddressResolver(EventExecutor executor) {
        this.executor = checkNotNull(executor, "executor");
        matcher = TypeParameterMatcher.find(this, AbstractAddressResolver.class, "T");
    }

    protected AbstractAddressResolver(EventExecutor executor, Class<? extends T> addressType) {
        this.executor = checkNotNull(executor, "executor");
        matcher = TypeParameterMatcher.get(addressType);
    }

    protected EventExecutor executor() {
        return executor;
    }

    @Override
    public boolean isSupported(SocketAddress address) {
        return matcher.match(address);
    }

    @Override
    public final boolean isResolved(SocketAddress address) {
        if (!isSupported(address)) {
            throw new UnsupportedAddressTypeException();
        }

        @SuppressWarnings("unchecked") final T castAddress = (T) address;
        return doIsResolved(castAddress);
    }

    protected abstract boolean doIsResolved(T address);

    @Override
    public final Future<T> resolve(SocketAddress address) {
        if (!isSupported(checkNotNull(address, "address"))) {

            return executor().newFailedFuture(new UnsupportedAddressTypeException());
        }

        if (isResolved(address)) {

            @SuppressWarnings("unchecked") final T cast = (T) address;
            return executor.newSucceededFuture(cast);
        }

        try {
            @SuppressWarnings("unchecked") final T cast = (T) address;
            final Promise<T> promise = executor().newPromise();
            doResolve(cast, promise);
            return promise;
        } catch (Exception e) {
            return executor().newFailedFuture(e);
        }
    }

    @Override
    public final Future<T> resolve(SocketAddress address, Promise<T> promise) {
        checkNotNull(address, "address");
        checkNotNull(promise, "promise");

        if (!isSupported(address)) {

            return promise.setFailure(new UnsupportedAddressTypeException());
        }

        if (isResolved(address)) {

            @SuppressWarnings("unchecked") final T cast = (T) address;
            return promise.setSuccess(cast);
        }

        try {
            @SuppressWarnings("unchecked") final T cast = (T) address;
            doResolve(cast, promise);
            return promise;
        } catch (Exception e) {
            return promise.setFailure(e);
        }
    }

    @Override
    public final Future<List<T>> resolveAll(SocketAddress address) {
        if (!isSupported(checkNotNull(address, "address"))) {

            return executor().newFailedFuture(new UnsupportedAddressTypeException());
        }

        if (isResolved(address)) {

            @SuppressWarnings("unchecked") final T cast = (T) address;
            return executor.newSucceededFuture(Collections.singletonList(cast));
        }

        try {
            @SuppressWarnings("unchecked") final T cast = (T) address;
            final Promise<List<T>> promise = executor().newPromise();
            doResolveAll(cast, promise);
            return promise;
        } catch (Exception e) {
            return executor().newFailedFuture(e);
        }
    }

    @Override
    public final Future<List<T>> resolveAll(SocketAddress address, Promise<List<T>> promise) {
        checkNotNull(address, "address");
        checkNotNull(promise, "promise");

        if (!isSupported(address)) {

            return promise.setFailure(new UnsupportedAddressTypeException());
        }

        if (isResolved(address)) {

            @SuppressWarnings("unchecked") final T cast = (T) address;
            return promise.setSuccess(Collections.singletonList(cast));
        }

        try {
            @SuppressWarnings("unchecked") final T cast = (T) address;
            doResolveAll(cast, promise);
            return promise;
        } catch (Exception e) {
            return promise.setFailure(e);
        }
    }

    protected abstract void doResolve(T unresolvedAddress, Promise<T> promise) throws Exception;

    protected abstract void doResolveAll(T unresolvedAddress, Promise<List<T>> promise) throws Exception;

    @Override
    public void close() {
    }
}
