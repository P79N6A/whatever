package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.ConfigConstants.EXPORT_KEY;
import static org.apache.dubbo.common.constants.ConfigConstants.REFER_KEY;

public abstract class AbstractRegistryFactory implements RegistryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final Map<String, Registry> REGISTRIES = new HashMap<>();

    public static Collection<Registry> getRegistries() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    public static void destroyAll() {
        if (LOGGER.isInfoEnabled()) {
            // [zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-consumer&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=1004&timestamp=1563110849042]
            // [redis://127.0.0.1:6379/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-consumer&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=6220&timestamp=1562560441186]
            // [redis://127.0.0.1:6379/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=4744&timestamp=1562563351247]
            LOGGER.info("Close all registries " + getRegistries());
        }
        LOCK.lock();
        try {
            for (Registry registry : getRegistries()) {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            REGISTRIES.clear();
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public Registry getRegistry(URL url) {
        url = URLBuilder.from(url).setPath(RegistryService.class.getName()).addParameter(INTERFACE_KEY, RegistryService.class.getName()).removeParameters(EXPORT_KEY, REFER_KEY).build();
        String key = url.toServiceStringWithoutResolving();
        LOCK.lock();
        try {
            // 访问缓存
            Registry registry = REGISTRIES.get(key);
            if (registry != null) {
                return registry;
            }
            // 缓存未命中，创建
            registry = createRegistry(url);
            if (registry == null) {
                throw new IllegalStateException("Can not create registry " + url);
            }
            // 写入缓存
            REGISTRIES.put(key, registry);
            return registry;
        } finally {
            LOCK.unlock();
        }
    }

    protected abstract Registry createRegistry(URL url);

}
