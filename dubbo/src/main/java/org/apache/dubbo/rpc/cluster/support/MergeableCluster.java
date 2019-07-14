package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;

public class MergeableCluster implements Cluster {

    public static final String NAME = "mergeable";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new MergeableClusterInvoker<T>(directory);
    }

}
