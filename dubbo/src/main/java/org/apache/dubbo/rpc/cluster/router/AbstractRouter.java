package org.apache.dubbo.rpc.cluster.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.cluster.Router;

/**
 * 服务目录在刷新Invoker列表的过程中，会通过Router进行服务路由，筛选出符合路由规则的服务提供者
 * 服务路由包含一条路由规则，路由规则决定了服务消费者的调用目标，即规定了服务消费者可调用哪些服务提供者
 */
public abstract class AbstractRouter implements Router {
    protected int priority = DEFAULT_PRIORITY;

    protected boolean force = false;

    protected URL url;

    protected DynamicConfiguration configuration;

    public AbstractRouter(DynamicConfiguration configuration, URL url) {
        this.configuration = configuration;
        this.url = url;
    }

    public AbstractRouter() {
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setConfiguration(DynamicConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}
