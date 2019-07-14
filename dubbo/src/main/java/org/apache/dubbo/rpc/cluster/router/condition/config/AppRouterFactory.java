package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;

@Activate(order = 200)
public class AppRouterFactory implements RouterFactory {
    public static final String NAME = "app";

    private volatile Router router;

    @Override
    public Router getRouter(URL url) {
        if (router != null) {
            return router;
        }
        synchronized (this) {
            if (router == null) {
                router = createRouter(url);
            }
        }
        return router;
    }

    private Router createRouter(URL url) {
        return new AppRouter(DynamicConfiguration.getDynamicConfiguration(), url);
    }

}
