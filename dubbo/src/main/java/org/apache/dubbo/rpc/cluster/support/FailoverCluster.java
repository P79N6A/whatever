package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;

public class FailoverCluster implements Cluster {

    public final static String NAME = "failover";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        // 创建并返回FailoverClusterInvoker对象
        return new FailoverClusterInvoker<T>(directory);
    }

}
