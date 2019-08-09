package org.springframework.beans.factory.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.lang.Nullable;

import java.util.Properties;

public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

    private boolean singleton = true;

    @Nullable
    private Properties properties;

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public boolean isSingleton() {
        return this.singleton;
    }

    @Override
    public void afterPropertiesSet() {
        if (isSingleton()) {
            this.properties = createProperties();
        }
    }

    @Override
    @Nullable
    public Properties getObject() {
        return (this.properties != null ? this.properties : createProperties());
    }

    @Override
    public Class<?> getObjectType() {
        return Properties.class;
    }

    protected Properties createProperties() {
        Properties result = CollectionFactory.createStringAdaptingProperties();
        process((properties, map) -> result.putAll(properties));
        return result;
    }

}
