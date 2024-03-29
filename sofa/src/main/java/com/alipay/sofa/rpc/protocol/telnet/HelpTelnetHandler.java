/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.protocol.telnet;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.protocol.TelnetHandler;
import com.alipay.sofa.rpc.protocol.TelnetHandlerFactory;
import com.alipay.sofa.rpc.transport.AbstractChannel;

import java.util.Map;

/**
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("help")
public class HelpTelnetHandler implements TelnetHandler {

    @Override
    public String getCommand() {
        return "help";
    }

    @Override
    public String telnet(AbstractChannel channel, String message) {
        StringBuffer result = new StringBuffer();
        if (StringUtils.isNotBlank(message)) {
            TelnetHandler handler = TelnetHandlerFactory.getHandler(message);
            if (handler != null) {
                result.append(handler.getCommand()).append(LINE).append(handler.getDescription()).append(LINE);
            } else {
                result.append("Not found command : " + message);
            }
        } else {
            result.append("The supported command include:").append(LINE);
            for (Map.Entry<String, TelnetHandler> entry : TelnetHandlerFactory.getAllHandlers().entrySet()) {
                result.append(entry.getKey()).append(" ");
                //result.append(entry.fetchKey() + "\t : " + entry.getValue().getDescription() + "\r\n");
            }
            result.append(LINE);
        }
        return result.toString();
    }

    @Override
    public String getDescription() {
        return "show all support commands!" + LINE + "Usage:\thelp" + LINE + "\thelp [cmd]";
    }

}
