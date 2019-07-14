package org.apache.dubbo.common.status.support;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.common.status.StatusChecker;

@Activate
public class MemoryStatusChecker implements StatusChecker {

    @Override
    public Status check() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();
        boolean ok = (maxMemory - (totalMemory - freeMemory) > 2048);
        String msg = "max:" + (maxMemory / 1024 / 1024) + "M,total:" + (totalMemory / 1024 / 1024) + "M,used:" + ((totalMemory / 1024 / 1024) - (freeMemory / 1024 / 1024)) + "M,free:" + (freeMemory / 1024 / 1024) + "M";
        return new Status(ok ? Status.Level.OK : Status.Level.WARN, msg);
    }

}
