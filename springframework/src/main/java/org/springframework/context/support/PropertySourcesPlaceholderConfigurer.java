package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

import java.io.IOException;
import java.util.Properties;

public class PropertySourcesPlaceholderConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

    public static final String LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME = "localProperties";

    public static final String ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME = "environmentProperties";

    @Nullable
    private MutablePropertySources propertySources;

    @Nullable
    private PropertySources appliedPropertySources;

    @Nullable
    private Environment environment;

    public void setPropertySources(PropertySources propertySources) {
        this.propertySources = new MutablePropertySources(propertySources);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (this.propertySources == null) {
            this.propertySources = new MutablePropertySources();
            // environmentProperties
            if (this.environment != null) {
                // 从Environment获取PropertySource注入（实现了EnvironmentAware接口）
                this.propertySources.addLast(new PropertySource<Environment>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
                    @Override
                    @Nullable
                    public String getProperty(String key) {
                        return this.source.getProperty(key);
                    }
                });
            }
            // localProperties
            try {
                PropertySource<?> localPropertySource = new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, mergeProperties());
                if (this.localOverride) {
                    this.propertySources.addFirst(localPropertySource);
                } else {
                    this.propertySources.addLast(localPropertySource);
                }
            } catch (IOException ex) {
                throw new BeanInitializationException("Could not load properties", ex);
            }
        }
        // 处理
        processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
        this.appliedPropertySources = this.propertySources;
    }

    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, final ConfigurablePropertyResolver propertyResolver) throws BeansException {
        // 前缀 ${
        propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
        // 后缀 }
        propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
        // 分隔符 :
        propertyResolver.setValueSeparator(this.valueSeparator);
        StringValueResolver valueResolver = strVal -> {
            // 默认false
            String resolved = (this.ignoreUnresolvablePlaceholders ? propertyResolver.resolvePlaceholders(strVal) : propertyResolver.resolveRequiredPlaceholders(strVal));
            if (this.trimValues) {
                resolved = resolved.trim();
            }
            return (resolved.equals(this.nullValue) ? null : resolved);
        };
        /*
         * 将BeanDefinition里边.xml配置的占位符${xxx.xxx}替换为.properties配置的xxx.xxx的值
         * 添加StringValueResolver到内置的EmbeddedValueResolver，注入依赖时用来解析@Value(value = "${xxx.xxx}")
         */
        doProcessProperties(beanFactoryToProcess, valueResolver);
    }

    @Override
    @Deprecated
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
        throw new UnsupportedOperationException("Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
    }

    public PropertySources getAppliedPropertySources() throws IllegalStateException {
        Assert.state(this.appliedPropertySources != null, "PropertySources have not yet been applied");
        return this.appliedPropertySources;
    }

}
