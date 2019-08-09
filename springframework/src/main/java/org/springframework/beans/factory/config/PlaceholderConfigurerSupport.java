package org.springframework.beans.factory.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

public abstract class PlaceholderConfigurerSupport extends PropertyResourceConfigurer implements BeanNameAware, BeanFactoryAware {

    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    public static final String DEFAULT_VALUE_SEPARATOR = ":";

    protected String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

    protected String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

    @Nullable
    protected String valueSeparator = DEFAULT_VALUE_SEPARATOR;

    protected boolean trimValues = false;

    @Nullable
    protected String nullValue;

    protected boolean ignoreUnresolvablePlaceholders = false;

    @Nullable
    private String beanName;

    @Nullable
    private BeanFactory beanFactory;

    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    public void setValueSeparator(@Nullable String valueSeparator) {
        this.valueSeparator = valueSeparator;
    }

    public void setTrimValues(boolean trimValues) {
        this.trimValues = trimValues;
    }

    public void setNullValue(String nullValue) {
        this.nullValue = nullValue;
    }

    public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess, StringValueResolver valueResolver) {
        BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);
        String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
        // 遍历
        for (String curName : beanNames) {
            // Check that we're not parsing our own bean definition,
            // to avoid failing on unresolvable placeholders in properties file locations.
            if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
                BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
                try {
                    // 替换占位符
                    visitor.visitBeanDefinition(bd);
                } catch (Exception ex) {
                    throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
                }
            }
        }
        // New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
        beanFactoryToProcess.resolveAliases(valueResolver);
        // New in Spring 3.0: resolve placeholders in embedded values such as annotation attributes.
        // 添加StringValueResolver到内置的EmbeddedValueResolver
        beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
    }

}
