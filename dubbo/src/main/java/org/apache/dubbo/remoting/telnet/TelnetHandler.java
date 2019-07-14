package org.apache.dubbo.remoting.telnet;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;

@SPI
public interface TelnetHandler {

    String telnet(Channel channel, String message) throws RemotingException;

}