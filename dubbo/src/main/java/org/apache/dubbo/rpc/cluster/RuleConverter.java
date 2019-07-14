package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

import java.util.List;

@SPI
public interface RuleConverter {

    List<URL> convert(URL subscribeUrl, Object source);

}
