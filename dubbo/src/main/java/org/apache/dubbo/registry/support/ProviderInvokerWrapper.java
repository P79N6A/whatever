package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

public class ProviderInvokerWrapper<T> implements Invoker {
    private Invoker<T> invoker;

    private URL originUrl;

    private URL registryUrl;

    private URL providerUrl;

    private volatile boolean isReg;

    public ProviderInvokerWrapper(Invoker<T> invoker, URL registryUrl, URL providerUrl) {
        this.invoker = invoker;
        this.originUrl = URL.valueOf(invoker.getUrl().toFullString());
        this.registryUrl = URL.valueOf(registryUrl.toFullString());
        this.providerUrl = providerUrl;
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void destroy() {
        invoker.destroy();
    }

    public URL getOriginUrl() {
        return originUrl;
    }

    public URL getRegistryUrl() {
        return registryUrl;
    }

    public URL getProviderUrl() {
        return providerUrl;
    }

    public Invoker<T> getInvoker() {
        return invoker;
    }

    public boolean isReg() {
        return isReg;
    }

    public void setReg(boolean reg) {
        isReg = reg;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ProviderInvokerWrapper)) {
            return false;
        }
        ProviderInvokerWrapper other = (ProviderInvokerWrapper) o;
        return other.getInvoker().equals(this.getInvoker());
    }

}
