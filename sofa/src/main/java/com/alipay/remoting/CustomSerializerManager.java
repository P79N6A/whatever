package com.alipay.remoting;

import java.util.concurrent.ConcurrentHashMap;

public class CustomSerializerManager {

    private static ConcurrentHashMap<String, CustomSerializer> classCustomSerializer = new ConcurrentHashMap<String, CustomSerializer>();

    private static ConcurrentHashMap<CommandCode, CustomSerializer> commandCustomSerializer = new ConcurrentHashMap<CommandCode, CustomSerializer>();

    public static void registerCustomSerializer(String className, CustomSerializer serializer) {
        CustomSerializer prevSerializer = classCustomSerializer.putIfAbsent(className, serializer);
        if (prevSerializer != null) {
            throw new RuntimeException("CustomSerializer has been registered for class: " + className + ", the custom serializer is: " + prevSerializer.getClass().getName());
        }
    }

    public static CustomSerializer getCustomSerializer(String className) {
        if (!classCustomSerializer.isEmpty()) {
            return classCustomSerializer.get(className);
        }
        return null;
    }

    public static void registerCustomSerializer(CommandCode code, CustomSerializer serializer) {
        CustomSerializer prevSerializer = commandCustomSerializer.putIfAbsent(code, serializer);
        if (prevSerializer != null) {
            throw new RuntimeException("CustomSerializer has been registered for command code: " + code + ", the custom serializer is: " + prevSerializer.getClass().getName());
        }
    }

    public static CustomSerializer getCustomSerializer(CommandCode code) {
        if (!commandCustomSerializer.isEmpty()) {
            return commandCustomSerializer.get(code);
        }
        return null;
    }

    public static void clear() {
        classCustomSerializer.clear();
        commandCustomSerializer.clear();
    }

}
