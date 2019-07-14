package org.apache.dubbo.remoting.telnet.support.command;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;

@Activate
@Help(parameter = "[lines]", summary = "Clear screen.", detail = "Clear screen.")
public class ClearTelnetHandler implements TelnetHandler {

    @Override
    public String telnet(Channel channel, String message) {
        int lines = 100;
        if (message.length() > 0) {
            if (!StringUtils.isInteger(message)) {
                return "Illegal lines " + message + ", must be integer.";
            }
            lines = Integer.parseInt(message);
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            buf.append("\r\n");
        }
        return buf.toString();
    }

}
