package com.alipay.remoting;

import com.alipay.remoting.config.*;
import com.alipay.remoting.config.configs.ConfigContainer;
import com.alipay.remoting.config.configs.ConfigItem;
import com.alipay.remoting.config.configs.ConfigType;
import com.alipay.remoting.config.configs.DefaultConfigContainer;
import com.alipay.remoting.config.switches.GlobalSwitch;
import com.alipay.remoting.log.BoltLoggerFactory;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public abstract class AbstractRemotingServer extends AbstractLifeCycle implements RemotingServer, ConfigurableInstance {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private String ip;

    private int port;

    private final BoltOptions options;

    private final ConfigType configType;

    private final GlobalSwitch globalSwitch;

    private final ConfigContainer configContainer;

    public AbstractRemotingServer(int port) {
        this(new InetSocketAddress(port).getAddress().getHostAddress(), port);
    }

    public AbstractRemotingServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.options = new BoltOptions();
        this.configType = ConfigType.SERVER_SIDE;
        this.globalSwitch = new GlobalSwitch();
        this.configContainer = new DefaultConfigContainer();
    }

    @Override
    @Deprecated
    public void init() {
    }

    @Override
    @Deprecated
    public boolean start() {
        startup();
        return true;
    }

    @Override
    @Deprecated
    public boolean stop() {
        shutdown();
        return true;
    }

    @Override
    public void startup() throws LifeCycleException {
        super.startup();
        try {
            doInit();
            logger.warn("Prepare to start server on port {} ", port);
            if (doStart()) {
                logger.warn("Server started on port {}", port);
            } else {
                logger.warn("Failed starting server on port {}", port);
                throw new LifeCycleException("Failed starting server on port: " + port);
            }
        } catch (Throwable t) {
            this.shutdown();
            throw new IllegalStateException("ERROR: Failed to start the Server!", t);
        }
    }

    @Override
    public void shutdown() throws LifeCycleException {
        super.shutdown();
        if (!doStop()) {
            throw new LifeCycleException("doStop fail");
        }
    }

    @Override
    public String ip() {
        return ip;
    }

    @Override
    public int port() {
        return port;
    }

    protected abstract void doInit();

    protected abstract boolean doStart() throws InterruptedException;

    protected abstract boolean doStop();

    @Override
    public <T> T option(BoltOption<T> option) {
        return options.option(option);
    }

    @Override
    public <T> Configurable option(BoltOption<T> option, T value) {
        options.option(option, value);
        return this;
    }

    @Override
    public ConfigContainer conf() {
        return this.configContainer;
    }

    @Override
    public GlobalSwitch switches() {
        return this.globalSwitch;
    }

    @Override
    public void initWriteBufferWaterMark(int low, int high) {
        this.configContainer.set(configType, ConfigItem.NETTY_BUFFER_LOW_WATER_MARK, low);
        this.configContainer.set(configType, ConfigItem.NETTY_BUFFER_HIGH_WATER_MARK, high);
    }

    @Override
    public int netty_buffer_low_watermark() {
        Object config = configContainer.get(configType, ConfigItem.NETTY_BUFFER_LOW_WATER_MARK);
        if (config != null) {
            return (Integer) config;
        } else {
            return ConfigManager.netty_buffer_low_watermark();
        }
    }

    @Override
    public int netty_buffer_high_watermark() {
        Object config = configContainer.get(configType, ConfigItem.NETTY_BUFFER_HIGH_WATER_MARK);
        if (config != null) {
            return (Integer) config;
        } else {
            return ConfigManager.netty_buffer_high_watermark();
        }
    }

}
