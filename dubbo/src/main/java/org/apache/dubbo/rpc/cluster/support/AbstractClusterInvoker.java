package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.rpc.cluster.Constants.*;

public abstract class AbstractClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClusterInvoker.class);

    protected final Directory<T> directory;

    protected final boolean availablecheck;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    private volatile Invoker<T> stickyInvoker = null;

    public AbstractClusterInvoker(Directory<T> directory) {
        this(directory, directory.getUrl());
    }

    public AbstractClusterInvoker(Directory<T> directory, URL url) {
        if (directory == null) {
            throw new IllegalArgumentException("service directory == null");
        }
        this.directory = directory;
        this.availablecheck = url.getParameter(CLUSTER_AVAILABLE_CHECK_KEY, DEFAULT_CLUSTER_AVAILABLE_CHECK);
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public URL getUrl() {
        return directory.getUrl();
    }

    @Override
    public boolean isAvailable() {
        Invoker<T> invoker = stickyInvoker;
        if (invoker != null) {
            return invoker.isAvailable();
        }
        return directory.isAvailable();
    }

    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            directory.destroy();
        }
    }

    /**
     * 主要逻辑集中在了对粘滞连接特性的支持上，尽可能让客户端总是同一提供者发起调用，除非该提供者挂了，再连另一台
     * 首先是获取sticky配置，然后再检测invokers列表中是否包含stickyInvoker，如果不包含，则认为该stickyInvoker不可用，将其置空
     * 这里的invokers列表可以看做是存活着的服务提供者列表，如果这个列表不包含stickyInvoker，自然认为stickyInvoker挂了
     * 如果stickyInvoker存在于invokers列表中，检测selected中是否包含stickyInvoker
     * 如果包含，说明stickyInvoker在此之前没有成功提供服务（但其仍然处于存活状态），此时认为这个服务不可靠，不该在重试期间内再次被调用，因此不会返回该stickyInvoker
     * 如果不包含，此时还需要进行可用性检测，比如检测服务提供者网络连通性等
     * 当可用性检测通过，才可返回stickyInvoker，否则调用doSelect方法选择Invoker
     * 如果sticky为true，此时会将doSelect方法选出的Invoker赋值给stickyInvoker
     */
    protected Invoker<T> select(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }
        // 调用方法名
        String methodName = invocation == null ? StringUtils.EMPTY : invocation.getMethodName();
        // sticky配置
        boolean sticky = invokers.get(0).getUrl().getMethodParameter(methodName, CLUSTER_STICKY_KEY, DEFAULT_CLUSTER_STICKY);
        // invokers列表不包含stickyInvoker，说明stickyInvoker挂了，将其置空
        if (stickyInvoker != null && !invokers.contains(stickyInvoker)) {
            stickyInvoker = null;
        }
        // sticky == true && stickyInvoker != null
        // 如果selected包含stickyInvoker，表明stickyInvoker对应的服务提供者可能因网络原因未能成功提供服务
        // 但是该提供者并没挂，此时invokers列表中仍存在该服务提供者对应的Invoker
        if (sticky && stickyInvoker != null && (selected == null || !selected.contains(stickyInvoker))) {
            // 开启了可用性检查 && 检查通过
            if (availablecheck && stickyInvoker.isAvailable()) {
                // 直接返回stickyInvoker
                return stickyInvoker;
            }
        }
        // 线程走到当前代码处，说明前面的stickyInvoker为空，或者不可用，继续调用doSelect选择Invoker
        Invoker<T> invoker = doSelect(loadbalance, invocation, invokers, selected);
        // 如果sticky为true，则将负载均衡组件选出的Invoker赋值给stickyInvoker
        if (sticky) {
            stickyInvoker = invoker;
        }
        return invoker;
    }


    private Invoker<T> doSelect(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        // 通过负载均衡组件选择Invoker
        Invoker<T> invoker = loadbalance.select(invokers, getUrl(), invocation);
        // 如果selected包含负载均衡选择出的Invoker，或者该Invoker无法经过可用性检查，此时重选
        if ((selected != null && selected.contains(invoker)) || (!invoker.isAvailable() && getUrl() != null && availablecheck)) {
            try {
                // 重选
                Invoker<T> rInvoker = reselect(loadbalance, invocation, invokers, selected, availablecheck);
                // 如果重选的rinvoker不为空，则将其赋值给 invoker
                if (rInvoker != null) {

                    invoker = rInvoker;
                }
                // 若reselect选出来的Invoker为空
                else {
                    // 定位invoker在invokers中的位置
                    int index = invokers.indexOf(invoker);
                    try {
                        // 获取 index + 1 处的Invoker
                        invoker = invokers.get((index + 1) % invokers.size());
                    } catch (Exception e) {
                        logger.warn(e.getMessage() + " may because invokers list dynamic change, ignore.", e);
                    }
                }
            } catch (Throwable t) {
                logger.error("cluster reselect fail reason is :" + t.getMessage() + " if can not solve, you can set cluster.availablecheck=false in url", t);
            }
        }
        return invoker;
    }


    private Invoker<T> reselect(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected, boolean availablecheck) throws RpcException {
        List<Invoker<T>> reselectInvokers = new ArrayList<>(invokers.size() > 1 ? (invokers.size() - 1) : invokers.size());
        for (Invoker<T> invoker : invokers) {
            // 跳过不可用
            if (availablecheck && !invoker.isAvailable()) {
                continue;
            }
            // 如果selected列表不包含当前invoker，则将其添加到reselectInvokers
            if (selected == null || !selected.contains(invoker)) {
                reselectInvokers.add(invoker);
            }
        }
        // reselectInvokers不为空，通过负载均衡再次选择
        if (!reselectInvokers.isEmpty()) {
            return loadbalance.select(reselectInvokers, getUrl(), invocation);
        }
        // 若线程走到此处，说明reselectInvokers集合为空，此时不会调用负载均衡组件筛选
        // 这里从selected列表中查找可用的Invoker，并将其添加到reselectInvokers集合中
        if (selected != null) {
            for (Invoker<T> invoker : selected) {
                if ((invoker.isAvailable()) && !reselectInvokers.contains(invoker)) {
                    reselectInvokers.add(invoker);
                }
            }
        }
        if (!reselectInvokers.isEmpty()) {
            // 再次进行选择，并返回选择结果
            return loadbalance.select(reselectInvokers, getUrl(), invocation);
        }
        return null;
    }

    @Override
    public Result invoke(final Invocation invocation) throws RpcException {
        checkWhetherDestroyed();
        // 绑定attachments到invocation
        Map<String, String> contextAttachments = RpcContext.getContext().getAttachments();
        if (contextAttachments != null && contextAttachments.size() != 0) {
            ((RpcInvocation) invocation).addAttachments(contextAttachments);
        }
        // 列举Invoker
        List<Invoker<T>> invokers = list(invocation);
        // 加载LoadBalance
        LoadBalance loadbalance = initLoadBalance(invokers, invocation);
        RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
        // 调用doInvoke
        return doInvoke(invocation, invokers, loadbalance);
    }

    protected void checkWhetherDestroyed() {
        if (destroyed.get()) {
            throw new RpcException("Rpc cluster invoker for " + getInterface() + " on consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + " is now destroyed! Can not invoke any more.");
        }
    }

    @Override
    public String toString() {
        return getInterface() + " -> " + getUrl().toString();
    }

    protected void checkInvokers(List<Invoker<T>> invokers, Invocation invocation) {
        if (CollectionUtils.isEmpty(invokers)) {
            throw new RpcException(RpcException.NO_INVOKER_AVAILABLE_AFTER_FILTER, "Failed to invoke the method " + invocation.getMethodName() + " in the service " + getInterface().getName() + ". No provider available for the service " + directory.getUrl().getServiceKey() + " from registry " + directory.getUrl().getAddress() + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version " + Version.getVersion() + ". Please check if the providers have been started and registered.");
        }
    }
    // 抽象方法，由子类实现
    protected abstract Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException;

    protected List<Invoker<T>> list(Invocation invocation) throws RpcException {
        // 调用Directory的list方法列举Invoker
        return directory.list(invocation);
    }

    protected LoadBalance initLoadBalance(List<Invoker<T>> invokers, Invocation invocation) {
        if (CollectionUtils.isNotEmpty(invokers)) {
            return ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl().getMethodParameter(RpcUtils.getMethodName(invocation), LOADBALANCE_KEY, DEFAULT_LOADBALANCE));
        } else {
            return ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(DEFAULT_LOADBALANCE);
        }
    }

}
