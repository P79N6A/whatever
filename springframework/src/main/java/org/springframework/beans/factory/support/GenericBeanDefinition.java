package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class GenericBeanDefinition extends AbstractBeanDefinition {

    @Nullable
    private String parentName;

    public GenericBeanDefinition() {
        super();
    }

    public GenericBeanDefinition(BeanDefinition original) {
        super(original);
    }

    @Override
    public void setParentName(@Nullable String parentName) {
        this.parentName = parentName;
    }

    @Override
    @Nullable
    public String getParentName() {
        return this.parentName;
    }

    @Override
    public AbstractBeanDefinition cloneBeanDefinition() {
        return new GenericBeanDefinition(this);
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof GenericBeanDefinition && super.equals(other)));
    }

    @Override
    public String toString() {
        if (this.parentName != null) {
            return "Generic bean with parent '" + this.parentName + "': " + super.toString();
        }
        return "Generic bean: " + super.toString();
    }

}
