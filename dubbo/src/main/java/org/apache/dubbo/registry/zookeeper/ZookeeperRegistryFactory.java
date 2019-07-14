package org.apache.dubbo.registry.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

public class ZookeeperRegistryFactory extends AbstractRegistryFactory {

    /**
     * 由SPI在运行时注入，类型为ZookeeperTransporter$Adaptive
     */
    private ZookeeperTransporter zookeeperTransporter;

    public void setZookeeperTransporter(ZookeeperTransporter zookeeperTransporter) {
        this.zookeeperTransporter = zookeeperTransporter;
    }

    @Override
    public Registry createRegistry(URL url) {
        // 创建ZookeeperRegistry
        return new ZookeeperRegistry(url, zookeeperTransporter);
    }

}
