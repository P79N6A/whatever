package com.alipay.remoting.rpc;

import com.alipay.remoting.CommonCommandCode;
import com.alipay.remoting.util.IDGenerator;

public class HeartbeatCommand extends RequestCommand {

    private static final long serialVersionUID = 4949981019109517725L;

    public HeartbeatCommand() {
        super(CommonCommandCode.HEARTBEAT);
        this.setId(IDGenerator.nextId());
    }

}
