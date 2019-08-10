package com.alipay.remoting;

import java.util.concurrent.Executor;

public interface InvokeCallback {

    void onResponse(final Object result);

    void onException(final Throwable e);

    Executor getExecutor();

}
