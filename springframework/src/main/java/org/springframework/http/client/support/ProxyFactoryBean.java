package org.springframework.http.client.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

public class ProxyFactoryBean implements FactoryBean<Proxy>, InitializingBean {

    private Proxy.Type type = Proxy.Type.HTTP;

    @Nullable
    private String hostname;

    private int port = -1;

    @Nullable
    private Proxy proxy;

    public void setType(Proxy.Type type) {
        this.type = type;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void afterPropertiesSet() throws IllegalArgumentException {
        Assert.notNull(this.type, "Property 'type' is required");
        Assert.notNull(this.hostname, "Property 'hostname' is required");
        if (this.port < 0 || this.port > 65535) {
            throw new IllegalArgumentException("Property 'port' value out of range: " + this.port);
        }
        SocketAddress socketAddress = new InetSocketAddress(this.hostname, this.port);
        this.proxy = new Proxy(this.type, socketAddress);
    }

    @Override
    @Nullable
    public Proxy getObject() {
        return this.proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return Proxy.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
