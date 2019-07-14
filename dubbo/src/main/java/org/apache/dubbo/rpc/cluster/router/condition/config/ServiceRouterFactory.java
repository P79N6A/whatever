package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.cluster.CacheableRouterFactory;
import org.apache.dubbo.rpc.cluster.Router;

@Activate(order = 300)
public class ServiceRouterFactory extends CacheableRouterFactory {

    public static final String NAME = "service";

    @Override
    protected Router createRouter(URL url) {
        return new ServiceRouter(DynamicConfiguration.getDynamicConfiguration(), url);
    }

}
