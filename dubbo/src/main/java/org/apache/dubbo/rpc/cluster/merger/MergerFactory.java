package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.cluster.Merger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MergerFactory {

    private static final ConcurrentMap<Class<?>, Merger<?>> MERGER_CACHE = new ConcurrentHashMap<Class<?>, Merger<?>>();

    public static <T> Merger<T> getMerger(Class<T> returnType) {
        if (returnType == null) {
            throw new IllegalArgumentException("returnType is null");
        }
        Merger result;
        if (returnType.isArray()) {
            Class type = returnType.getComponentType();
            result = MERGER_CACHE.get(type);
            if (result == null) {
                loadMergers();
                result = MERGER_CACHE.get(type);
            }
            if (result == null && !type.isPrimitive()) {
                result = ArrayMerger.INSTANCE;
            }
        } else {
            result = MERGER_CACHE.get(returnType);
            if (result == null) {
                loadMergers();
                result = MERGER_CACHE.get(returnType);
            }
        }
        return result;
    }

    static void loadMergers() {
        Set<String> names = ExtensionLoader.getExtensionLoader(Merger.class).getSupportedExtensions();
        for (String name : names) {
            Merger m = ExtensionLoader.getExtensionLoader(Merger.class).getExtension(name);
            MERGER_CACHE.putIfAbsent(ReflectUtils.getGenericClass(m.getClass()), m);
        }
    }

}
