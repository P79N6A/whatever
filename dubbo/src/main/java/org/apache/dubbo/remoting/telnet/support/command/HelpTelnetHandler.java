package org.apache.dubbo.remoting.telnet.support.command;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;
import org.apache.dubbo.remoting.telnet.support.TelnetUtils;

import java.util.ArrayList;
import java.util.List;

@Activate
@Help(parameter = "[command]", summary = "Show help.", detail = "Show help.")
public class HelpTelnetHandler implements TelnetHandler {

    private final ExtensionLoader<TelnetHandler> extensionLoader = ExtensionLoader.getExtensionLoader(TelnetHandler.class);

    @Override
    public String telnet(Channel channel, String message) {
        if (message.length() > 0) {
            if (!extensionLoader.hasExtension(message)) {
                return "No such command " + message;
            }
            TelnetHandler handler = extensionLoader.getExtension(message);
            Help help = handler.getClass().getAnnotation(Help.class);
            StringBuilder buf = new StringBuilder();
            buf.append("Command:\r\n    ");
            buf.append(message + " " + help.parameter().replace("\r\n", " ").replace("\n", " "));
            buf.append("\r\nSummary:\r\n    ");
            buf.append(help.summary().replace("\r\n", " ").replace("\n", " "));
            buf.append("\r\nDetail:\r\n    ");
            buf.append(help.detail().replace("\r\n", "    \r\n").replace("\n", "    \n"));
            return buf.toString();
        } else {
            List<List<String>> table = new ArrayList<List<String>>();
            List<TelnetHandler> handlers = extensionLoader.getActivateExtension(channel.getUrl(), "telnet");
            if (CollectionUtils.isNotEmpty(handlers)) {
                for (TelnetHandler handler : handlers) {
                    Help help = handler.getClass().getAnnotation(Help.class);
                    List<String> row = new ArrayList<String>();
                    String parameter = " " + extensionLoader.getExtensionName(handler) + " " + (help != null ? help.parameter().replace("\r\n", " ").replace("\n", " ") : "");
                    row.add(parameter.length() > 55 ? parameter.substring(0, 55) + "..." : parameter);
                    String summary = help != null ? help.summary().replace("\r\n", " ").replace("\n", " ") : "";
                    row.add(summary.length() > 55 ? summary.substring(0, 55) + "..." : summary);
                    table.add(row);
                }
            }
            return "Please input \"help [command]\" show detail.\r\n" + TelnetUtils.toList(table);
        }
    }

}
