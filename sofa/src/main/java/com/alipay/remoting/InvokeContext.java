package com.alipay.remoting;

import java.util.concurrent.ConcurrentHashMap;

public class InvokeContext {

    public final static String CLIENT_LOCAL_IP = "bolt.client.local.ip";

    public final static String CLIENT_LOCAL_PORT = "bolt.client.local.port";

    public final static String CLIENT_REMOTE_IP = "bolt.client.remote.ip";

    public final static String CLIENT_REMOTE_PORT = "bolt.client.remote.port";

    public final static String CLIENT_CONN_CREATETIME = "bolt.client.conn.createtime";

    public final static String SERVER_LOCAL_IP = "bolt.server.local.ip";

    public final static String SERVER_LOCAL_PORT = "bolt.server.local.port";

    public final static String SERVER_REMOTE_IP = "bolt.server.remote.ip";

    public final static String SERVER_REMOTE_PORT = "bolt.server.remote.port";

    public final static String BOLT_INVOKE_REQUEST_ID = "bolt.invoke.request.id";

    public final static String BOLT_PROCESS_WAIT_TIME = "bolt.invoke.wait.time";

    public final static String BOLT_CUSTOM_SERIALIZER = "bolt.invoke.custom.serializer";

    public final static String BOLT_CRC_SWITCH = "bolt.invoke.crc.switch";

    public final static int INITIAL_SIZE = 8;

    private ConcurrentHashMap<String, Object> context;

    public InvokeContext() {
        this.context = new ConcurrentHashMap<String, Object>(INITIAL_SIZE);
    }

    public void putIfAbsent(String key, Object value) {
        this.context.putIfAbsent(key, value);
    }

    public void put(String key, Object value) {
        this.context.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) this.context.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultIfNotFound) {
        return this.context.get(key) != null ? (T) this.context.get(key) : defaultIfNotFound;
    }

    public void clear() {
        this.context.clear();
    }

}