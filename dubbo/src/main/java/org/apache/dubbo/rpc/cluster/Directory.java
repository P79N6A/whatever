package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.Node;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

public interface Directory<T> extends Node {

    Class<T> getInterface();

    List<Invoker<T>> list(Invocation invocation) throws RpcException;

}