package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.configcenter.DynamicConfiguration;

public class AppRouter extends ListenableRouter {
    public static final String NAME = "APP_ROUTER";

    private static final int APP_ROUTER_DEFAULT_PRIORITY = 150;

    public AppRouter(DynamicConfiguration configuration, URL url) {
        super(configuration, url, url.getParameter(CommonConstants.APPLICATION_KEY));
        this.priority = APP_ROUTER_DEFAULT_PRIORITY;
    }

}
