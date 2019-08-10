package com.alipay.remoting.rpc;

import com.alipay.remoting.config.ConfigManager;

public class RpcConfigManager {
    public static boolean dispatch_msg_list_in_default_executor() {
        return ConfigManager.getBool(RpcConfigs.DISPATCH_MSG_LIST_IN_DEFAULT_EXECUTOR, RpcConfigs.DISPATCH_MSG_LIST_IN_DEFAULT_EXECUTOR_DEFAULT);
    }

}
