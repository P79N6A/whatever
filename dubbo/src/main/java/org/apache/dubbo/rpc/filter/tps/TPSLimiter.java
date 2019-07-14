package org.apache.dubbo.rpc.filter.tps;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

public interface TPSLimiter {

    boolean isAllowable(URL url, Invocation invocation);

}
