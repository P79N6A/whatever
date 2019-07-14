package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public abstract class AbstractProtocol implements Protocol {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>();

    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<Invoker<?>>();

    protected static String serviceKey(URL url) {
        int port = url.getParameter(RemotingConstants.BIND_PORT_KEY, url.getPort());
        return serviceKey(port, url.getPath(), url.getParameter(VERSION_KEY), url.getParameter(GROUP_KEY));
    }

    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }

    @Override
    public void destroy() {
        for (Invoker<?> invoker : invokers) {
            if (invoker != null) {
                invokers.remove(invoker);
                try {
                    if (logger.isInfoEnabled()) {
                        // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-consumer&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=6220&register=true&register.ip=192.168.1.108&remote.application=dubbo-demo-api-provider&revision=1.0.0&sayHello.timeout=1000&side=consumer&sticky=false&timestamp=1562557776866&version=1.0.0
                        // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-consumer&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=6412&register=true&register.ip=192.168.1.108&remote.application=dubbo-demo-api-provider&revision=1.0.0&sayHello.timeout=1000&side=consumer&sticky=false&timestamp=1562554187077&version=1.0.0
                        // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-consumer&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=6412&register=true&register.ip=192.168.1.108&remote.application=dubbo-demo-api-provider&revision=1.0.0&sayHello.timeout=1000&side=consumer&sticky=false&timestamp=1562557776866&version=1.0.0
                        logger.info("Destroy reference: " + invoker.getUrl());
                    }
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        for (String key : new ArrayList<String>(exporterMap.keySet())) {
            Exporter<?> exporter = exporterMap.remove(key);
            if (exporter != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&bind.ip=192.168.1.108&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=4744&register=true&release=&revision=1.0.0&sayHello.timeout=1000&side=provider&timestamp=1562563351261&version=1.0.0
                        // injvm://127.0.0.1/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&bind.ip=192.168.1.108&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=4744&register=true&release=&revision=1.0.0&sayHello.timeout=1000&side=provider&timestamp=1562563351261&version=1.0.0
                        logger.info("Unexport service: " + exporter.getInvoker().getUrl());
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

}
