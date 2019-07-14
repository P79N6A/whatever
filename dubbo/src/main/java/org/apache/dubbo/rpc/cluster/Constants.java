package org.apache.dubbo.rpc.cluster;

public interface Constants {
    String LOADBALANCE_KEY = "loadbalance";

    String DEFAULT_LOADBALANCE = "random";

    String FAIL_BACK_TASKS_KEY = "failbacktasks";

    int DEFAULT_FAILBACK_TASKS = 100;

    String RETRIES_KEY = "retries";

    int DEFAULT_RETRIES = 2;

    int DEFAULT_FAILBACK_TIMES = 3;

    String FORKS_KEY = "forks";

    int DEFAULT_FORKS = 2;

    String WEIGHT_KEY = "weight";

    int DEFAULT_WEIGHT = 100;

    String MOCK_PROTOCOL = "mock";

    String FORCE_KEY = "force";

    String CLUSTER_AVAILABLE_CHECK_KEY = "cluster.availablecheck";

    boolean DEFAULT_CLUSTER_AVAILABLE_CHECK = true;

    String CLUSTER_STICKY_KEY = "sticky";

    boolean DEFAULT_CLUSTER_STICKY = false;

    String ADDRESS_KEY = "address";

    String INVOCATION_NEED_MOCK = "invocation.need.mock";

    String ROUTER_TYPE_CLEAR = "clean";

    String DEFAULT_SCRIPT_TYPE_KEY = "javascript";

    String PRIORITY_KEY = "priority";

    String RULE_KEY = "rule";

    String TYPE_KEY = "type";

    String RUNTIME_KEY = "runtime";

    String REMOTE_TIMESTAMP_KEY = "remote.timestamp";

    String WARMUP_KEY = "warmup";

    int DEFAULT_WARMUP = 10 * 60 * 1000;

    String CONFIG_VERSION_KEY = "configVersion";

    String OVERRIDE_PROVIDERS_KEY = "providerAddresses";

    String TAG_KEY = "dubbo.tag";

}
