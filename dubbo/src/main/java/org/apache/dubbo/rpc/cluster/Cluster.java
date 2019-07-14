package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.support.FailoverCluster;

/**
 * 集群工作过程可分为两个阶段
 * 第一个阶段是在服务消费者初始化期间，集群Cluster实现类为服务消费者创建Cluster Invoker实例
 * 第二个阶段是在服务消费者进行远程调用时
 * 以FailoverClusterInvoker为例，该类型Cluster Invoker首先会调用Directory的list方法列举Invoker列表（可将Invoker简单理解为服务提供者）
 * RegistryDirectory是一个动态服务目录，可感知注册中心配置的变化，它所持有的Invoker列表会随着注册中心内容的变化而变化
 * 每次变化后，RegistryDirectory会动态增删Invoker，并调用Router的route方法进行路由，过滤掉不符合路由规则的Invoker
 * 当FailoverClusterInvoker拿到Directory返回的Invoker列表后，它会通过LoadBalance从Invoker列表中选择一个Invoker
 * 最后FailoverClusterInvoker会将参数传给LoadBalance选择出的Invoker实例的invoker方法，进行真正的远程调用
 */
@SPI(FailoverCluster.NAME)
public interface Cluster {

    @Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;

}