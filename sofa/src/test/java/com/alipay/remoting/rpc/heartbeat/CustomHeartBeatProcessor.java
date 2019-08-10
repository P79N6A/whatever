package com.alipay.remoting.rpc.heartbeat;

import com.alipay.remoting.AbstractRemotingProcessor;
import com.alipay.remoting.RemotingCommand;
import com.alipay.remoting.RemotingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomHeartBeatProcessor extends AbstractRemotingProcessor<RemotingCommand> {
    static Logger logger = LoggerFactory.getLogger(CustomHeartBeatProcessor.class);

    private AtomicInteger heartBeatTimes = new AtomicInteger();

    public int getHeartBeatTimes() {
        return heartBeatTimes.get();
    }

    public void reset() {
        this.heartBeatTimes.set(0);
    }

    @Override
    public void doProcess(RemotingContext ctx, RemotingCommand msg) throws Exception {
        heartBeatTimes.incrementAndGet();
        java.text.DateFormat format1 = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        logger.warn("heart beat received:" + format1.format(new Date()));
    }

}
