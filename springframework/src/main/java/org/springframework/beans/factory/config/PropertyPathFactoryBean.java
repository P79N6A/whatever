package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class PropertyPathFactoryBean implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware {

    private static final Log logger = LogFactory.getLog(PropertyPathFactoryBean.class);

    @Nullable
    private BeanWrapper targetBeanWrapper;

    @Nullable
    private String targetBeanName;

    @Nullable
    private String propertyPath;

    @Nullable
    private Class<?> resultType;

    @Nullable
    private String beanName;

    @Nullable
    private BeanFactory beanFactory;

    public void setTargetObject(Object targetObject) {
        this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(targetObject);
    }

    public void setTargetBeanName(String targetBeanName) {
        this.targetBeanName = StringUtils.trimAllWhitespace(targetBeanName);
    }

    public void setPropertyPath(String propertyPath) {
        this.propertyPath = StringUtils.trimAllWhitespace(propertyPath);
    }

    public void setResultType(Class<?> resultType) {
        this.resultType = resultType;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (this.targetBeanWrapper != null && this.targetBeanName != null) {
            throw new IllegalArgumentException("Specify either 'targetObject' or 'targetBeanName', not both");
        }
        if (this.targetBeanWrapper == null && this.targetBeanName == null) {
            if (this.propertyPath != null) {
                throw new IllegalArgumentException("Specify 'targetObject' or 'targetBeanName' in combination with 'propertyPath'");
            }
            // No other properties specified: check bean name.
            int dotIndex = (this.beanName != null ? this.beanName.indexOf('.') : -1);
            if (dotIndex == -1) {
                throw new IllegalArgumentException("Neither 'targetObject' nor 'targetBeanName' specified, and PropertyPathFactoryBean " + "bean name '" + this.beanName + "' does not follow 'beanName.property' syntax");
            }
            this.targetBeanName = this.beanName.substring(0, dotIndex);
            this.propertyPath = this.beanName.substring(dotIndex + 1);
        } else if (this.propertyPath == null) {
            // either targetObject or targetBeanName specified
            throw new IllegalArgumentException("'propertyPath' is required");
        }
        if (this.targetBeanWrapper == null && this.beanFactory.isSingleton(this.targetBeanName)) {
            // Eagerly fetch singleton target bean, and determine result type.
            Object bean = this.beanFactory.getBean(this.targetBeanName);
            this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
            this.resultType = this.targetBeanWrapper.getPropertyType(this.propertyPath);
        }
    }

    @Override
    @Nullable
    public Object getObject() throws BeansException {
        BeanWrapper target = this.targetBeanWrapper;
        if (target != null) {
            if (logger.isWarnEnabled() && this.targetBeanName != null && this.beanFactory instanceof ConfigurableBeanFactory && ((ConfigurableBeanFactory) this.beanFactory).isCurrentlyInCreation(this.targetBeanName)) {
                logger.warn("Target bean '" + this.targetBeanName + "' is still in creation due to a circular " + "reference - obtained value for property '" + this.propertyPath + "' may be outdated!");
            }
        } else {
            // Fetch prototype target bean...
            Assert.state(this.beanFactory != null, "No BeanFactory available");
            Assert.state(this.targetBeanName != null, "No target bean name specified");
            Object bean = this.beanFactory.getBean(this.targetBeanName);
            target = PropertyAccessorFactory.forBeanPropertyAccess(bean);
        }
        Assert.state(this.propertyPath != null, "No property path specified");
        return target.getPropertyValue(this.propertyPath);
    }

    @Override
    public Class<?> getObjectType() {
        return this.resultType;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

}
