package org.apache.dubbo.remoting.zookeeper.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

public abstract class AbstractZookeeperTransporter implements ZookeeperTransporter {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTransporter.class);

    private final Map<String, ZookeeperClient> zookeeperClientMap = new ConcurrentHashMap<>();

    @Override
    public ZookeeperClient connect(URL url) {
        ZookeeperClient zookeeperClient;
        List<String> addressList = getURLBackupAddress(url);
        if ((zookeeperClient = fetchAndUpdateZookeeperClientCache(addressList)) != null && zookeeperClient.isConnected()) {
            // zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.registry.RegistryService
            // &pid=1976
            // &timestamp=1562825799431
            logger.info("find valid zookeeper client from the cache for address: " + url);
            return zookeeperClient;
        }
        synchronized (zookeeperClientMap) {
            if ((zookeeperClient = fetchAndUpdateZookeeperClientCache(addressList)) != null && zookeeperClient.isConnected()) {
                logger.info("find valid zookeeper client from the cache for address: " + url);
                return zookeeperClient;
            }
            zookeeperClient = createZookeeperClient(toClientURL(url));
            // zookeeper://127.0.0.1:2181/dubbo/ConfigCenterConfig?
            // address=zookeeper://127.0.0.1:2181
            // &check=true
            // &configFile=dubbo.properties
            // &group=dubbo
            // &highestPriority=false
            // &namespace=dubbo
            // &prefix=dubbo.config-center
            // &timeout=3000
            // &valid=true
            logger.info("No valid zookeeper client found from cache, therefore create a new client for url. " + url);
            writeToClientMap(addressList, zookeeperClient);
        }
        return zookeeperClient;
    }

    protected abstract ZookeeperClient createZookeeperClient(URL url);

    ZookeeperClient fetchAndUpdateZookeeperClientCache(List<String> addressList) {
        ZookeeperClient zookeeperClient = null;
        for (String address : addressList) {
            if ((zookeeperClient = zookeeperClientMap.get(address)) != null && zookeeperClient.isConnected()) {
                break;
            }
        }
        if (zookeeperClient != null && zookeeperClient.isConnected()) {
            writeToClientMap(addressList, zookeeperClient);
        }
        return zookeeperClient;
    }

    List<String> getURLBackupAddress(URL url) {
        List<String> addressList = new ArrayList<String>();
        addressList.add(url.getAddress());
        addressList.addAll(url.getParameter(RemotingConstants.BACKUP_KEY, Collections.EMPTY_LIST));
        return addressList;
    }

    void writeToClientMap(List<String> addressList, ZookeeperClient zookeeperClient) {
        for (String address : addressList) {
            zookeeperClientMap.put(address, zookeeperClient);
        }
    }

    URL toClientURL(URL url) {
        Map<String, String> parameterMap = new HashMap<>();
        if (url.getParameter(TIMEOUT_KEY) != null) {
            parameterMap.put(TIMEOUT_KEY, url.getParameter(TIMEOUT_KEY));
        }
        if (url.getParameter(RemotingConstants.BACKUP_KEY) != null) {
            parameterMap.put(RemotingConstants.BACKUP_KEY, url.getParameter(RemotingConstants.BACKUP_KEY));
        }
        return new URL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), ZookeeperTransporter.class.getName(), parameterMap);
    }

    Map<String, ZookeeperClient> getZookeeperClientMap() {
        return zookeeperClientMap;
    }

}
