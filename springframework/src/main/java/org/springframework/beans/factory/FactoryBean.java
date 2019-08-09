package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

public interface FactoryBean<T> {

    /**
     * 获取FactoryBean初始化的Bean实例
     */
    @Nullable
    T getObject() throws Exception;

    /**
     * 获取Bean实例的类型
     */
    @Nullable
    Class<?> getObjectType();

    /**
     * 是否是单例模式，决定是否基于缓存将它维护成一个单例对象
     */
    default boolean isSingleton() {
        return true;
    }

}
