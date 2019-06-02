package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.IdentityHashMap;
import java.util.Map;

@UnstableApi
public abstract class AddressResolverGroup<T extends SocketAddress> implements Closeable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AddressResolverGroup.class);

    private final Map<EventExecutor, AddressResolver<T>> resolvers = new IdentityHashMap<EventExecutor, AddressResolver<T>>();

    protected AddressResolverGroup() {
    }

    public AddressResolver<T> getResolver(final EventExecutor executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }

        if (executor.isShuttingDown()) {
            throw new IllegalStateException("executor not accepting a task");
        }

        AddressResolver<T> r;
        synchronized (resolvers) {
            r = resolvers.get(executor);
            if (r == null) {
                final AddressResolver<T> newResolver;
                try {
                    newResolver = newResolver(executor);
                } catch (Exception e) {
                    throw new IllegalStateException("failed to create a new resolver", e);
                }

                resolvers.put(executor, newResolver);
                executor.terminationFuture().addListener(new FutureListener<Object>() {
                    @Override
                    public void operationComplete(Future<Object> future) throws Exception {
                        synchronized (resolvers) {
                            resolvers.remove(executor);
                        }
                        newResolver.close();
                    }
                });

                r = newResolver;
            }
        }

        return r;
    }

    protected abstract AddressResolver<T> newResolver(EventExecutor executor) throws Exception;

    @Override
    @SuppressWarnings({"unchecked", "SuspiciousToArrayCall"})
    public void close() {
        final AddressResolver<T>[] rArray;
        synchronized (resolvers) {
            rArray = (AddressResolver<T>[]) resolvers.values().toArray(new AddressResolver[0]);
            resolvers.clear();
        }

        for (AddressResolver<T> r : rArray) {
            try {
                r.close();
            } catch (Throwable t) {
                logger.warn("Failed to close a resolver:", t);
            }
        }
    }
}
