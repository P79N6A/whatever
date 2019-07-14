package org.apache.dubbo.common.status;

import org.apache.dubbo.common.extension.SPI;

@SPI
public interface StatusChecker {

    Status check();

}