package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

@SPI
public interface ConfiguratorFactory {

    @Adaptive("protocol")
    Configurator getConfigurator(URL url);

}
