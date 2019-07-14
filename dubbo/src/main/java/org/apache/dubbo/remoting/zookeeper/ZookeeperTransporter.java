package org.apache.dubbo.remoting.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

@SPI("curator")
public interface ZookeeperTransporter {

    @Adaptive({RemotingConstants.CLIENT_KEY, RemotingConstants.TRANSPORTER_KEY})
    ZookeeperClient connect(URL url);

}
