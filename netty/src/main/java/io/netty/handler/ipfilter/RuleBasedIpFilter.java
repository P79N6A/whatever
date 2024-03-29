package io.netty.handler.ipfilter;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

@Sharable
public class RuleBasedIpFilter extends AbstractRemoteAddressFilter<InetSocketAddress> {

    private final IpFilterRule[] rules;

    public RuleBasedIpFilter(IpFilterRule... rules) {
        if (rules == null) {
            throw new NullPointerException("rules");
        }

        this.rules = rules;
    }

    @Override
    protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception {
        for (IpFilterRule rule : rules) {
            if (rule == null) {
                break;
            }

            if (rule.matches(remoteAddress)) {
                return rule.ruleType() == IpFilterRuleType.ACCEPT;
            }
        }

        return true;
    }
}
