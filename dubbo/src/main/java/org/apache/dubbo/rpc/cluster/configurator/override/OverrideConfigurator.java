package org.apache.dubbo.rpc.cluster.configurator.override;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.configurator.AbstractConfigurator;

public class OverrideConfigurator extends AbstractConfigurator {

    public OverrideConfigurator(URL url) {
        super(url);
    }

    @Override
    public URL doConfigure(URL currentUrl, URL configUrl) {
        return currentUrl.addParameters(configUrl.getParameters());
    }

}
