package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;

import java.io.IOException;

public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    @Nullable
    private Boolean allowBeanDefinitionOverriding;

    @Nullable
    private Boolean allowCircularReferences;

    @Nullable
    private DefaultListableBeanFactory beanFactory;

    private final Object beanFactoryMonitor = new Object();

    public AbstractRefreshableApplicationContext() {
    }

    public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
        super(parent);
    }

    public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
        this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
    }

    public void setAllowCircularReferences(boolean allowCircularReferences) {
        this.allowCircularReferences = allowCircularReferences;
    }

    @Override
    protected final void refreshBeanFactory() throws BeansException {
        // 如果ApplicationContext已有BeanFactory，销毁Bean，关闭BeanFactory
        if (hasBeanFactory()) {
            destroyBeans();
            closeBeanFactory();
        }
        try {
            // 新建DefaultListableBeanFactory
            DefaultListableBeanFactory beanFactory = createBeanFactory();
            // 序列化
            beanFactory.setSerializationId(getId());
            // 配置BeanFactory，是否允许Bean覆盖、是否允许循环引用
            customizeBeanFactory(beanFactory);
            // 加载BeanDefinition到BeanFactory
            loadBeanDefinitions(beanFactory);
            synchronized (this.beanFactoryMonitor) {
                this.beanFactory = beanFactory;
            }
        } catch (IOException ex) {
            throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
        }
    }

    @Override
    protected void cancelRefresh(BeansException ex) {
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory != null) {
                this.beanFactory.setSerializationId(null);
            }
        }
        super.cancelRefresh(ex);
    }

    @Override
    protected final void closeBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory != null) {
                this.beanFactory.setSerializationId(null);
                this.beanFactory = null;
            }
        }
    }

    protected final boolean hasBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            return (this.beanFactory != null);
        }
    }

    @Override
    public final ConfigurableListableBeanFactory getBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory == null) {
                throw new IllegalStateException("BeanFactory not initialized or already closed - " + "call 'refresh' before accessing beans via the ApplicationContext");
            }
            return this.beanFactory;
        }
    }

    @Override
    protected void assertBeanFactoryActive() {
    }

    protected DefaultListableBeanFactory createBeanFactory() {
        return new DefaultListableBeanFactory(getInternalParentBeanFactory());
    }

    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        // 是否允许Bean定义覆盖
        if (this.allowBeanDefinitionOverriding != null) {
            beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
        }
        // 是否允许Bean循环依赖
        if (this.allowCircularReferences != null) {
            beanFactory.setAllowCircularReferences(this.allowCircularReferences);
        }
    }

    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException;

}
