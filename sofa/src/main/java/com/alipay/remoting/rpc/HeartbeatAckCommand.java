package com.alipay.remoting.rpc;

import com.alipay.remoting.CommonCommandCode;
import com.alipay.remoting.ResponseStatus;

public class HeartbeatAckCommand extends ResponseCommand {

    private static final long serialVersionUID = 2584912495844320855L;

    public HeartbeatAckCommand() {
        super(CommonCommandCode.HEARTBEAT);
        this.setResponseStatus(ResponseStatus.SUCCESS);
    }

}
