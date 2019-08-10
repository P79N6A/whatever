package com.alipay.remoting;

public interface Protocol {

    CommandEncoder getEncoder();

    CommandDecoder getDecoder();

    HeartbeatTrigger getHeartbeatTrigger();

    CommandHandler getCommandHandler();

    CommandFactory getCommandFactory();

}
