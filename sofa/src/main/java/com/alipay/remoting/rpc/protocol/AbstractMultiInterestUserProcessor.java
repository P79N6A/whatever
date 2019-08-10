package com.alipay.remoting.rpc.protocol;

public abstract class AbstractMultiInterestUserProcessor<T> extends AbstractUserProcessor<T> implements MultiInterestUserProcessor<T> {

    @Override
    public String interest() {
        return null;
    }

}
