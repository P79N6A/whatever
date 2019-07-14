package org.apache.dubbo.common.timer;

import java.util.concurrent.TimeUnit;

public interface TimerTask {

    void run(Timeout timeout) throws Exception;

}