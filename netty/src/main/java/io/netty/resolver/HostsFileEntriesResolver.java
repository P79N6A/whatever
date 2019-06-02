package io.netty.resolver;

import io.netty.util.internal.UnstableApi;

import java.net.InetAddress;

@UnstableApi
public interface HostsFileEntriesResolver {

    HostsFileEntriesResolver DEFAULT = new DefaultHostsFileEntriesResolver();

    InetAddress address(String inetHost, ResolvedAddressTypes resolvedAddressTypes);
}
