package org.apache.dubbo.common.threadpool;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

import java.util.concurrent.Executor;

import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;

@SPI("fixed")
public interface ThreadPool {

    @Adaptive({THREADPOOL_KEY})
    Executor getExecutor(URL url);

}
