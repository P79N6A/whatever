package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public abstract class PropertyResourceConfigurer extends PropertiesLoaderSupport implements BeanFactoryPostProcessor, PriorityOrdered {

    private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            Properties mergedProps = mergeProperties();
            // Convert the merged properties, if necessary.
            convertProperties(mergedProps);
            // Let the subclass process the properties.
            processProperties(beanFactory, mergedProps);
        } catch (IOException ex) {
            throw new BeanInitializationException("Could not load properties", ex);
        }
    }

    protected void convertProperties(Properties props) {
        Enumeration<?> propertyNames = props.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();
            String propertyValue = props.getProperty(propertyName);
            String convertedValue = convertProperty(propertyName, propertyValue);
            if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
                props.setProperty(propertyName, convertedValue);
            }
        }
    }

    protected String convertProperty(String propertyName, String propertyValue) {
        return convertPropertyValue(propertyValue);
    }

    protected String convertPropertyValue(String originalValue) {
        return originalValue;
    }

    protected abstract void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) throws BeansException;

}
