package org.apache.dubbo.remoting;

public interface Constants {

    String BUFFER_KEY = "buffer";

    int DEFAULT_BUFFER_SIZE = 8 * 1024;

    int MAX_BUFFER_SIZE = 16 * 1024;

    int MIN_BUFFER_SIZE = 1 * 1024;

    String IDLE_TIMEOUT_KEY = "idle.timeout";

    int DEFAULT_IDLE_TIMEOUT = 600 * 1000;

    String ACCEPTS_KEY = "accepts";

    int DEFAULT_ACCEPTS = 0;

    String CONNECT_QUEUE_CAPACITY = "connect.queue.capacity";

    String CONNECT_QUEUE_WARNING_SIZE = "connect.queue.warning.size";

    int DEFAULT_CONNECT_QUEUE_WARNING_SIZE = 1000;

    String CHARSET_KEY = "charset";

    String DEFAULT_CHARSET = "UTF-8";

    int HEARTBEAT_CHECK_TICK = 3;

    long LEAST_HEARTBEAT_DURATION = 1000;

    int TICKS_PER_WHEEL = 128;

}
