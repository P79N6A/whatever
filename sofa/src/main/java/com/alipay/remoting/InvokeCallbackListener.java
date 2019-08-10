package com.alipay.remoting;

public interface InvokeCallbackListener {

    void onResponse(final InvokeFuture future);

    String getRemoteAddress();

}
