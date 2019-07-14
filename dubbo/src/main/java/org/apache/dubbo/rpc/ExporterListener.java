package org.apache.dubbo.rpc;

import org.apache.dubbo.common.extension.SPI;

@SPI
public interface ExporterListener {

    void exported(Exporter<?> exporter) throws RpcException;

    void unexported(Exporter<?> exporter);

}