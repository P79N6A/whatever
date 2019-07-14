package org.apache.dubbo.rpc.cluster.configurator.absent;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;

public class AbsentConfiguratorFactory implements ConfiguratorFactory {

    @Override
    public Configurator getConfigurator(URL url) {
        return new AbsentConfigurator(url);
    }

}
