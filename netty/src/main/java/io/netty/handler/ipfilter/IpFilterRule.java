package io.netty.handler.ipfilter;

import java.net.InetSocketAddress;

public interface IpFilterRule {

    boolean matches(InetSocketAddress remoteAddress);

    IpFilterRuleType ruleType();
}
