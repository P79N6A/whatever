package com.alipay.remoting.rpc.userprocessor.multiinterestprocessor;

import java.io.Serializable;

public interface MultiInterestBaseRequestBody extends Serializable {

    int getId();

    void setId(int id);

    String getMsg();

    void setMsg(String msg);

}
