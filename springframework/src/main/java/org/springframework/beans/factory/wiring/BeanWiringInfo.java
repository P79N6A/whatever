package org.springframework.beans.factory.wiring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class BeanWiringInfo {

    public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

    public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

    @Nullable
    private String beanName;

    private boolean isDefaultBeanName = false;

    private int autowireMode = AutowireCapableBeanFactory.AUTOWIRE_NO;

    private boolean dependencyCheck = false;

    public BeanWiringInfo() {
    }

    public BeanWiringInfo(String beanName) {
        this(beanName, false);
    }

    public BeanWiringInfo(String beanName, boolean isDefaultBeanName) {
        Assert.hasText(beanName, "'beanName' must not be empty");
        this.beanName = beanName;
        this.isDefaultBeanName = isDefaultBeanName;
    }

    public BeanWiringInfo(int autowireMode, boolean dependencyCheck) {
        if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
            throw new IllegalArgumentException("Only constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE supported");
        }
        this.autowireMode = autowireMode;
        this.dependencyCheck = dependencyCheck;
    }

    public boolean indicatesAutowiring() {
        return (this.beanName == null);
    }

    @Nullable
    public String getBeanName() {
        return this.beanName;
    }

    public boolean isDefaultBeanName() {
        return this.isDefaultBeanName;
    }

    public int getAutowireMode() {
        return this.autowireMode;
    }

    public boolean getDependencyCheck() {
        return this.dependencyCheck;
    }

}
