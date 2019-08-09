package org.springframework.beans.factory.parsing;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BeanComponentDefinition extends BeanDefinitionHolder implements ComponentDefinition {

    private BeanDefinition[] innerBeanDefinitions;

    private BeanReference[] beanReferences;

    public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName) {
        this(new BeanDefinitionHolder(beanDefinition, beanName));
    }

    public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
        this(new BeanDefinitionHolder(beanDefinition, beanName, aliases));
    }

    public BeanComponentDefinition(BeanDefinitionHolder beanDefinitionHolder) {
        super(beanDefinitionHolder);
        List<BeanDefinition> innerBeans = new ArrayList<>();
        List<BeanReference> references = new ArrayList<>();
        PropertyValues propertyValues = beanDefinitionHolder.getBeanDefinition().getPropertyValues();
        for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
            Object value = propertyValue.getValue();
            if (value instanceof BeanDefinitionHolder) {
                innerBeans.add(((BeanDefinitionHolder) value).getBeanDefinition());
            } else if (value instanceof BeanDefinition) {
                innerBeans.add((BeanDefinition) value);
            } else if (value instanceof BeanReference) {
                references.add((BeanReference) value);
            }
        }
        this.innerBeanDefinitions = innerBeans.toArray(new BeanDefinition[0]);
        this.beanReferences = references.toArray(new BeanReference[0]);
    }

    @Override
    public String getName() {
        return getBeanName();
    }

    @Override
    public String getDescription() {
        return getShortDescription();
    }

    @Override
    public BeanDefinition[] getBeanDefinitions() {
        return new BeanDefinition[]{getBeanDefinition()};
    }

    @Override
    public BeanDefinition[] getInnerBeanDefinitions() {
        return this.innerBeanDefinitions;
    }

    @Override
    public BeanReference[] getBeanReferences() {
        return this.beanReferences;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof BeanComponentDefinition && super.equals(other)));
    }

}
