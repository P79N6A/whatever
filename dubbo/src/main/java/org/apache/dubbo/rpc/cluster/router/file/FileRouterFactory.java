package org.apache.dubbo.rpc.cluster.router.file;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;
import org.apache.dubbo.rpc.cluster.router.script.ScriptRouterFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.apache.dubbo.common.constants.RegistryConstants.ROUTER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.*;

public class FileRouterFactory implements RouterFactory {

    public static final String NAME = "file";

    private RouterFactory routerFactory;

    public void setRouterFactory(RouterFactory routerFactory) {
        this.routerFactory = routerFactory;
    }

    @Override
    public Router getRouter(URL url) {
        try {
            String protocol = url.getParameter(ROUTER_KEY, ScriptRouterFactory.NAME);
            String type = null;
            String path = url.getPath();
            if (path != null) {
                int i = path.lastIndexOf('.');
                if (i > 0) {
                    type = path.substring(i + 1);
                }
            }
            String rule = IOUtils.read(new FileReader(new File(url.getAbsolutePath())));
            boolean runtime = url.getParameter(RUNTIME_KEY, false);
            URL script = URLBuilder.from(url).setProtocol(protocol).addParameter(TYPE_KEY, type).addParameter(RUNTIME_KEY, runtime).addParameterAndEncoded(RULE_KEY, rule).build();
            return routerFactory.getRouter(script);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
