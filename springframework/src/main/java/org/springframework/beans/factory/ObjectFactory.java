package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * FactoryBean与ObjectFactory都是用工厂模式来创建对象
 * <p>
 * FactoryBean：
 * 自定义创建对象过程，由BeanFactory通过FactoryBean来获取目标对象
 * ObjectFactory：
 * 在doGetBean时，创建对象的过程由框架通过ObjectFactory定义，创建的时机交给拓展接口Scope
 * 此外在将给依赖注入列表注册一个ObjectFactory类型的对象，在注入过程中会调用objectFactory#getObject创建目标对象注入进去
 * 如beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectFactory());）
 */
@FunctionalInterface
public interface ObjectFactory<T> {

    T getObject() throws BeansException;

}
