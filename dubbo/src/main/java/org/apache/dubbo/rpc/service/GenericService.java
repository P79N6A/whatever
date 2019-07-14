package org.apache.dubbo.rpc.service;

/**
 * 泛化调用
 * 服务消费者端因为某种原因并没有该服务接口
 */
public interface GenericService {

    Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;

}