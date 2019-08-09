package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.lang.Nullable;

/**
 * BeanDefinition
 * RootBeanDefinition，ChildBeanDefinition和GenericBeanDefinition都继承了AbstractBeanDefinition
 * GenericBeanDefinition：一般定义用户可见的Bean
 * RootBeanDefinition/ChildBeanDefinition：父子关系已确定
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

    /**
     * 默认只有SINGLETON、PROTOTYPE两种，其它属于拓展
     */
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

    /**
     * APPLICATION – 应用程序的主要部分，通常对应用户定义的Bean
     */
    int ROLE_APPLICATION = 0;

    /**
     * SUPPORT - 应用支持
     */
    int ROLE_SUPPORT = 1;

    /**
     * INFRASTRUCTURE - 基础设施，仅用于框架内部
     */
    int ROLE_INFRASTRUCTURE = 2;

    // Modifiable attributes

    void setParentName(@Nullable String parentName);

    /**
     * 父Bean，用来继承父Bean的配置信息
     */
    @Nullable
    String getParentName();

    void setBeanClassName(@Nullable String beanClassName);

    /**
     * Bean的类名
     * 该属性并不总是运行时对应Bean真正使用的类的名称
     * Bean是通过某个类的静态工厂方法生成的，为类名
     * Bean是通过FactoryBean生成的，为空
     */
    @Nullable
    String getBeanClassName();

    void setScope(@Nullable String scope);

    /**
     * Scope
     */
    @Nullable
    String getScope();


    void setLazyInit(boolean lazyInit);

    /**
     * 是否懒加载
     */
    boolean isLazyInit();


    void setDependsOn(@Nullable String... dependsOn);

    /**
     * 该Bean的所有依赖，不是指属性依赖(如@Autowire标记的)，而是depends-on=""设置的
     */
    @Nullable
    String[] getDependsOn();

    void setAutowireCandidate(boolean autowireCandidate);

    /**
     * 该Bean是否可以注入到其他Bean中，只对根据类型注入有效
     */
    boolean isAutowireCandidate();


    void setPrimary(boolean primary);

    /**
     * 主要的，同一接口多个实现，不指定名字则优先选择Primary
     */
    boolean isPrimary();


    void setFactoryBeanName(@Nullable String factoryBeanName);

    /**
     * 工厂名称，如果该Bean采用工厂方法生成，有些实例不是用反射，而是用工厂模式生成的
     */
    @Nullable
    String getFactoryBeanName();

    void setFactoryMethodName(@Nullable String factoryMethodName);

    /**
     * 工厂类中的工厂方法名称
     * 基于类静态工厂方法时，结合beanClassName使用
     * 基于FactoryBean时，结合factoryBeanName使用
     * 如果constructorArgumentValues有内容，工厂方法被调用时会使用该属性
     */
    @Nullable
    String getFactoryMethodName();

    /**
     * 构造器参数
     */
    ConstructorArgumentValues getConstructorArgumentValues();

    default boolean hasConstructorArgumentValues() {
        return !getConstructorArgumentValues().isEmpty();
    }

    /**
     * Bean中的属性值，注入属性
     */
    MutablePropertyValues getPropertyValues();

    default boolean hasPropertyValues() {
        return !getPropertyValues().isEmpty();
    }

    void setInitMethodName(@Nullable String initMethodName);

    /**
     *
     */
    @Nullable
    String getInitMethodName();

    void setDestroyMethodName(@Nullable String destroyMethodName);

    /**
     *
     */
    @Nullable
    String getDestroyMethodName();

    void setRole(int role);

    /**
     * 角色
     */
    int getRole();

    void setDescription(@Nullable String description);

    @Nullable
    String getDescription();

    // Read-only attributes

    boolean isSingleton();


    boolean isPrototype();

    /**
     * Abstract不能实例化，一般作为父Bean用于继承
     */
    boolean isAbstract();

    @Nullable
    String getResourceDescription();

    @Nullable
    BeanDefinition getOriginatingBeanDefinition();

}
