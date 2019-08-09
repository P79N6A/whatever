package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.inject.Provider;
import java.io.Serializable;

public class ProviderCreatingFactoryBean extends AbstractFactoryBean<Provider<Object>> {

    @Nullable
    private String targetBeanName;

    public void setTargetBeanName(String targetBeanName) {
        this.targetBeanName = targetBeanName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
        super.afterPropertiesSet();
    }

    @Override
    public Class<?> getObjectType() {
        return Provider.class;
    }

    @Override
    protected Provider<Object> createInstance() {
        BeanFactory beanFactory = getBeanFactory();
        Assert.state(beanFactory != null, "No BeanFactory available");
        Assert.state(this.targetBeanName != null, "No target bean name specified");
        return new TargetBeanProvider(beanFactory, this.targetBeanName);
    }

    @SuppressWarnings("serial")
    private static class TargetBeanProvider implements Provider<Object>, Serializable {

        private final BeanFactory beanFactory;

        private final String targetBeanName;

        public TargetBeanProvider(BeanFactory beanFactory, String targetBeanName) {
            this.beanFactory = beanFactory;
            this.targetBeanName = targetBeanName;
        }

        @Override
        public Object get() throws BeansException {
            return this.beanFactory.getBean(this.targetBeanName);
        }

    }

}
