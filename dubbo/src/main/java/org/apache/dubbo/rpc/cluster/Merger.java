package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.SPI;

@SPI
public interface Merger<T> {

    T merge(T... items);

}
