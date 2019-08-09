package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

import java.util.Set;

/**
 * 自动装配Bean
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

    int AUTOWIRE_NO = 0;

    int AUTOWIRE_BY_NAME = 1;

    int AUTOWIRE_BY_TYPE = 2;

    int AUTOWIRE_CONSTRUCTOR = 3;

    @Deprecated
    int AUTOWIRE_AUTODETECT = 4;

    String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";
    //-------------------------------------------------------------------------
    // Typical methods for creating and populating external bean instances
    //-------------------------------------------------------------------------

    <T> T createBean(Class<T> beanClass) throws BeansException;

    void autowireBean(Object existingBean) throws BeansException;

    Object configureBean(Object existingBean, String beanName) throws BeansException;
    //-------------------------------------------------------------------------
    // Specialized methods for fine-grained control over the bean lifecycle
    //-------------------------------------------------------------------------

    Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

    Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

    void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) throws BeansException;

    void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

    Object initializeBean(Object existingBean, String beanName) throws BeansException;

    Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException;

    Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException;

    void destroyBean(Object existingBean);
    //-------------------------------------------------------------------------
    // Delegate methods for resolving injection points
    //-------------------------------------------------------------------------

    <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

    Object resolveBeanByName(String name, DependencyDescriptor descriptor) throws BeansException;

    @Nullable
    Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException;

    @Nullable
    Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName, @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException;

}
