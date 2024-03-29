package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.configcenter.DynamicConfiguration;

public class ServiceRouter extends ListenableRouter {
    public static final String NAME = "SERVICE_ROUTER";

    private static final int SERVICE_ROUTER_DEFAULT_PRIORITY = 140;

    public ServiceRouter(DynamicConfiguration configuration, URL url) {
        super(configuration, url, url.getEncodedServiceKey());
        this.priority = SERVICE_ROUTER_DEFAULT_PRIORITY;
    }

}
