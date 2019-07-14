package org.apache.dubbo.rpc.cluster.configurator.absent;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.configurator.AbstractConfigurator;

public class AbsentConfigurator extends AbstractConfigurator {

    public AbsentConfigurator(URL url) {
        super(url);
    }

    @Override
    public URL doConfigure(URL currentUrl, URL configUrl) {
        return currentUrl.addParametersIfAbsent(configUrl.getParameters());
    }

}
