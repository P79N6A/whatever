package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.integration.RegistryDirectory;
import org.apache.dubbo.rpc.Invoker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProviderConsumerRegTable {
    public static ConcurrentHashMap<String, ConcurrentMap<Invoker, ProviderInvokerWrapper>> providerInvokers = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Set<ConsumerInvokerWrapper>> consumerInvokers = new ConcurrentHashMap<>();

    public static <T> ProviderInvokerWrapper<T> registerProvider(Invoker<T> invoker, URL registryUrl, URL providerUrl) {
        ProviderInvokerWrapper<T> wrapperInvoker = new ProviderInvokerWrapper<>(invoker, registryUrl, providerUrl);
        String serviceUniqueName = providerUrl.getServiceKey();
        ConcurrentMap<Invoker, ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            providerInvokers.putIfAbsent(serviceUniqueName, new ConcurrentHashMap<>());
            invokers = providerInvokers.get(serviceUniqueName);
        }
        invokers.put(invoker, wrapperInvoker);
        return wrapperInvoker;
    }

    public static Set<ProviderInvokerWrapper> getProviderInvoker(String serviceUniqueName) {
        ConcurrentMap<Invoker, ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(invokers.values());
    }

    public static <T> ProviderInvokerWrapper<T> getProviderWrapper(URL registeredProviderUrl, Invoker<T> invoker) {
        String serviceUniqueName = registeredProviderUrl.getServiceKey();
        ConcurrentMap<Invoker, ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            return null;
        }
        for (Map.Entry<Invoker, ProviderInvokerWrapper> entry : invokers.entrySet()) {
            if (entry.getKey() == invoker) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static void registerConsumer(Invoker invoker, URL registryUrl, URL consumerUrl, RegistryDirectory registryDirectory) {
        ConsumerInvokerWrapper wrapperInvoker = new ConsumerInvokerWrapper(invoker, registryUrl, consumerUrl, registryDirectory);
        String serviceUniqueName = consumerUrl.getServiceKey();
        Set<ConsumerInvokerWrapper> invokers = consumerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            consumerInvokers.putIfAbsent(serviceUniqueName, new ConcurrentHashSet<ConsumerInvokerWrapper>());
            invokers = consumerInvokers.get(serviceUniqueName);
        }
        invokers.add(wrapperInvoker);
    }

    public static Set<ConsumerInvokerWrapper> getConsumerInvoker(String serviceUniqueName) {
        Set<ConsumerInvokerWrapper> invokers = consumerInvokers.get(serviceUniqueName);
        return invokers == null ? Collections.emptySet() : invokers;
    }

    public static boolean isRegistered(String serviceUniqueName) {
        Set<ProviderInvokerWrapper> providerInvokerWrapperSet = ProviderConsumerRegTable.getProviderInvoker(serviceUniqueName);
        return providerInvokerWrapperSet.stream().anyMatch(ProviderInvokerWrapper::isReg);
    }

    public static int getConsumerAddressNum(String serviceUniqueName) {
        Set<ConsumerInvokerWrapper> providerInvokerWrapperSet = ProviderConsumerRegTable.getConsumerInvoker(serviceUniqueName);
        return providerInvokerWrapperSet.stream().map(w -> w.getRegistryDirectory().getUrlInvokerMap()).filter(Objects::nonNull).mapToInt(Map::size).sum();
    }

}
