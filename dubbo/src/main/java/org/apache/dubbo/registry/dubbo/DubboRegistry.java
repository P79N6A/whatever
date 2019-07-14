package org.apache.dubbo.registry.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_RECONNECT_PERIOD_KEY;

public class DubboRegistry extends FailbackRegistry {

    private final static Logger logger = LoggerFactory.getLogger(DubboRegistry.class);

    private static final int RECONNECT_PERIOD_DEFAULT = 3 * 1000;

    private final ScheduledExecutorService reconnectTimer = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryReconnectTimer", true));

    private final ScheduledFuture<?> reconnectFuture;

    private final ReentrantLock clientLock = new ReentrantLock();

    private final Invoker<RegistryService> registryInvoker;

    private final RegistryService registryService;

    private final int reconnectPeriod;

    public DubboRegistry(Invoker<RegistryService> registryInvoker, RegistryService registryService) {
        super(registryInvoker.getUrl());
        this.registryInvoker = registryInvoker;
        this.registryService = registryService;
        this.reconnectPeriod = registryInvoker.getUrl().getParameter(REGISTRY_RECONNECT_PERIOD_KEY, RECONNECT_PERIOD_DEFAULT);
        reconnectFuture = reconnectTimer.scheduleWithFixedDelay(() -> {
            try {
                connect();
            } catch (Throwable t) {
                logger.error("Unexpected error occur at reconnect, cause: " + t.getMessage(), t);
            }
        }, reconnectPeriod, reconnectPeriod, TimeUnit.MILLISECONDS);
    }

    protected final void connect() {
        try {
            if (isAvailable()) {
                return;
            }
            if (logger.isInfoEnabled()) {
                logger.info("Reconnect to registry " + getUrl());
            }
            clientLock.lock();
            try {
                if (isAvailable()) {
                    return;
                }
                recover();
            } finally {
                clientLock.unlock();
            }
        } catch (Throwable t) {
            if (getUrl().getParameter(RemotingConstants.CHECK_KEY, true)) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                throw new RuntimeException(t.getMessage(), t);
            }
            logger.error("Failed to connect to registry " + getUrl().getAddress() + " from provider/consumer " + NetUtils.getLocalHost() + " use dubbo " + Version.getVersion() + ", cause: " + t.getMessage(), t);
        }
    }

    @Override
    public boolean isAvailable() {
        if (registryInvoker == null) {
            return false;
        }
        return registryInvoker.isAvailable();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            ExecutorUtil.cancelScheduledFuture(reconnectFuture);
        } catch (Throwable t) {
            logger.warn("Failed to cancel reconnect timer", t);
        }
        registryInvoker.destroy();
        ExecutorUtil.gracefulShutdown(reconnectTimer, reconnectPeriod);
    }

    @Override
    public void doRegister(URL url) {
        registryService.register(url);
    }

    @Override
    public void doUnregister(URL url) {
        registryService.unregister(url);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        registryService.subscribe(url, listener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        registryService.unsubscribe(url, listener);
    }

    @Override
    public List<URL> lookup(URL url) {
        return registryService.lookup(url);
    }

}
