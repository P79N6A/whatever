package com.alipay.remoting.rpc.protocol;

import java.util.List;

public interface MultiInterestUserProcessor<T> extends UserProcessor<T> {

    List<String> multiInterest();

}
