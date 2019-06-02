package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.UnstableApi;

import java.net.InetSocketAddress;

@UnstableApi
public final class DefaultAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {

    public static final DefaultAddressResolverGroup INSTANCE = new DefaultAddressResolverGroup();

    private DefaultAddressResolverGroup() {
    }

    @Override
    protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) throws Exception {
        return new DefaultNameResolver(executor).asAddressResolver();
    }
}
