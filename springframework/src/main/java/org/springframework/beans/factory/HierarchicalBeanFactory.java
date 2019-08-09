package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * 多个BeanFactory，父子关系
 */
public interface HierarchicalBeanFactory extends BeanFactory {

    @Nullable
    BeanFactory getParentBeanFactory();

    boolean containsLocalBean(String name);

}
