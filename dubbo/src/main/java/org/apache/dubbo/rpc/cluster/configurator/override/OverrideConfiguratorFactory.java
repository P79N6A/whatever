package org.apache.dubbo.rpc.cluster.configurator.override;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;

public class OverrideConfiguratorFactory implements ConfiguratorFactory {

    @Override
    public Configurator getConfigurator(URL url) {
        return new OverrideConfigurator(url);
    }

}
