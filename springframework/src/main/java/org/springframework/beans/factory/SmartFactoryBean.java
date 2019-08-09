package org.springframework.beans.factory;

public interface SmartFactoryBean<T> extends FactoryBean<T> {

    default boolean isPrototype() {
        return false;
    }

    default boolean isEagerInit() {
        return false;
    }

}
