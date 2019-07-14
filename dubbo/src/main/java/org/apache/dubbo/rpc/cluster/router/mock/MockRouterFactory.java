package org.apache.dubbo.rpc.cluster.router.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;

@Activate
public class MockRouterFactory implements RouterFactory {
    public static final String NAME = "mock";

    @Override
    public Router getRouter(URL url) {
        return new MockInvokersSelector();
    }

}
