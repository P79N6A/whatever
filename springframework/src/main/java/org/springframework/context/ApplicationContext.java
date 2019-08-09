package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * Bean生命周期
 * 1、Bean自身的方法：init-method和destroy-method指定的方法
 * 2、Bean级生命周期接口方法：BeanNameAware、BeanFactoryAware、InitializingBean、DisposableBean接口方法
 * 3、容器级生命周期接口方法：InstantiationAwareBeanPostProcessor、BeanPostProcessor接口方法
 * 4、工厂后处理器接口方法：AspectJWeavingEnabler、ConfigurationClassPostProcessor、CustomAutowireConfigurer等接口方法，容器级，在应用上下文装配配置文件后立即调用
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory, MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

    @Nullable
    String getId();

    String getApplicationName();

    String getDisplayName();

    long getStartupDate();

    @Nullable
    ApplicationContext getParent();

    AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
