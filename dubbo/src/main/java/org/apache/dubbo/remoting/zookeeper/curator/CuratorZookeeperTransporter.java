package org.apache.dubbo.remoting.zookeeper.curator;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.support.AbstractZookeeperTransporter;

public class CuratorZookeeperTransporter extends AbstractZookeeperTransporter {
    @Override
    public ZookeeperClient createZookeeperClient(URL url) {
        return new CuratorZookeeperClient(url);
    }

}
