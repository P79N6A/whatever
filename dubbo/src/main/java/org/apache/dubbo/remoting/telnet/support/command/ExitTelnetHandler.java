package org.apache.dubbo.remoting.telnet.support.command;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;

@Activate
@Help(parameter = "", summary = "Exit the telnet.", detail = "Exit the telnet.")
public class ExitTelnetHandler implements TelnetHandler {

    @Override
    public String telnet(Channel channel, String message) {
        channel.close();
        return null;
    }

}
