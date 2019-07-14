package org.apache.dubbo.rpc.support;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.RpcContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class AccessLogData {

    private static final String MESSAGE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final DateFormat MESSAGE_DATE_FORMATTER = new SimpleDateFormat(MESSAGE_DATE_FORMAT);

    private static final String VERSION = "version";

    private static final String GROUP = "group";

    private static final String SERVICE = "service";

    private static final String METHOD_NAME = "method-name";

    private static final String INVOCATION_TIME = "invocation-time";

    private static final String TYPES = "types";

    private static final String ARGUMENTS = "arguments";

    private static final String REMOTE_HOST = "remote-host";

    private static final String REMOTE_PORT = "remote-port";

    private static final String LOCAL_HOST = "localhost";

    private static final String LOCAL_PORT = "local-port";

    private Map<String, Object> data;

    private AccessLogData() {
        RpcContext context = RpcContext.getContext();
        data = new HashMap<>();
        setLocalHost(context.getLocalHost());
        setLocalPort(context.getLocalPort());
        setRemoteHost(context.getRemoteHost());
        setRemotePort(context.getRemotePort());
    }

    public static AccessLogData newLogData() {
        return new AccessLogData();
    }

    public void setVersion(String version) {
        set(VERSION, version);
    }

    public void setServiceName(String serviceName) {
        set(SERVICE, serviceName);
    }

    public void setGroup(String group) {
        set(GROUP, group);
    }

    public void setInvocationTime(Date invocationTime) {
        set(INVOCATION_TIME, invocationTime);
    }

    private void setRemoteHost(String remoteHost) {
        set(REMOTE_HOST, remoteHost);
    }

    private void setRemotePort(Integer remotePort) {
        set(REMOTE_PORT, remotePort);
    }

    private void setLocalHost(String localHost) {
        set(LOCAL_HOST, localHost);
    }

    private void setLocalPort(Integer localPort) {
        set(LOCAL_PORT, localPort);
    }

    public void setMethodName(String methodName) {
        set(METHOD_NAME, methodName);
    }

    public void setTypes(Class[] types) {
        set(TYPES, types != null ? Arrays.copyOf(types, types.length) : null);
    }

    public void setArguments(Object[] arguments) {
        set(ARGUMENTS, arguments != null ? Arrays.copyOf(arguments, arguments.length) : null);
    }

    public String getServiceName() {
        return get(SERVICE).toString();
    }

    public String getLogMessage() {
        StringBuilder sn = new StringBuilder();
        sn.append("[").append(MESSAGE_DATE_FORMATTER.format(getInvocationTime())).append("] ").append(get(REMOTE_HOST)).append(":").append(get(REMOTE_PORT)).append(" -> ").append(get(LOCAL_HOST)).append(":").append(get(LOCAL_PORT)).append(" - ");
        String group = get(GROUP) != null ? get(GROUP).toString() : "";
        if (StringUtils.isNotEmpty(group.toString())) {
            sn.append(group).append("/");
        }
        sn.append(get(SERVICE));
        String version = get(VERSION) != null ? get(VERSION).toString() : "";
        if (StringUtils.isNotEmpty(version.toString())) {
            sn.append(":").append(version);
        }
        sn.append(" ");
        sn.append(get(METHOD_NAME));
        sn.append("(");
        Class<?>[] types = get(TYPES) != null ? (Class<?>[]) get(TYPES) : new Class[0];
        boolean first = true;
        for (Class<?> type : types) {
            if (first) {
                first = false;
            } else {
                sn.append(",");
            }
            sn.append(type.getName());
        }
        sn.append(") ");
        Object[] args = get(ARGUMENTS) != null ? (Object[]) get(ARGUMENTS) : null;
        if (args != null && args.length > 0) {
            sn.append(JSON.toJSONString(args));
        }
        return sn.toString();
    }

    private Date getInvocationTime() {
        return (Date) get(INVOCATION_TIME);
    }

    private Object get(String key) {
        return data.get(key);
    }

    private void set(String key, Object value) {
        data.put(key, value);
    }

}
