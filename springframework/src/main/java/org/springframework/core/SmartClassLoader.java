package org.springframework.core;

public interface SmartClassLoader {

    boolean isClassReloadable(Class<?> clazz);

}
