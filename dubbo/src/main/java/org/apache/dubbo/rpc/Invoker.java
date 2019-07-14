package org.apache.dubbo.rpc;

import org.apache.dubbo.common.Node;

/**
 * Invoker可分为三类：
 * AbstractProxyInvoker：本地执行类的Invoker，实际通过Java反射的方式执行原始对象的方法
 * AbstractInvoker：远程通信类的Invoker，实际通过通信协议发起远程调用请求并接收响应
 * AbstractClusterInvoker：多个远程通信类的Invoker聚合成的集群版Invoker，加入了集群容错和负载均衡策略
 */
public interface Invoker<T> extends Node {

    Class<T> getInterface();

    Result invoke(Invocation invocation) throws RpcException;

}