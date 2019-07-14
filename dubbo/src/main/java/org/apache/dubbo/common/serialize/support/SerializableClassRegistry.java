package org.apache.dubbo.common.serialize.support;

import com.esotericsoftware.kryo.Serializer;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SerializableClassRegistry {

    private static final Map<Class, Object> REGISTRATIONS = new LinkedHashMap<>();

    public static void registerClass(Class clazz) {
        registerClass(clazz, null);
    }

    public static void registerClass(Class clazz, Serializer serializer) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class registered to kryo cannot be null!");
        }
        REGISTRATIONS.put(clazz, serializer);
    }

    public static Map<Class, Object> getRegisteredClasses() {
        return REGISTRATIONS;
    }

}
