package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.*;

public class ProtocolUtils {

    private ProtocolUtils() {
    }

    public static String serviceKey(URL url) {
        return serviceKey(url.getPort(), url.getPath(), url.getParameter(VERSION_KEY), url.getParameter(GROUP_KEY));
    }

    public static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotEmpty(serviceGroup)) {
            buf.append(serviceGroup);
            buf.append("/");
        }
        buf.append(serviceName);
        if (serviceVersion != null && serviceVersion.length() > 0 && !"0.0.0".equals(serviceVersion)) {
            buf.append(":");
            buf.append(serviceVersion);
        }
        buf.append(":");
        buf.append(port);
        return buf.toString();
    }

    public static boolean isGeneric(String generic) {
        return generic != null && !"".equals(generic) && (GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic) || GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic) || GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic) || GENERIC_SERIALIZATION_PROTOBUF.equalsIgnoreCase(generic));
    }

    public static boolean isDefaultGenericSerialization(String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic);
    }

    public static boolean isJavaGenericSerialization(String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic);
    }

    public static boolean isBeanGenericSerialization(String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_BEAN.equals(generic);
    }

    public static boolean isProtobufGenericSerialization(String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_PROTOBUF.equals(generic);
    }

}
