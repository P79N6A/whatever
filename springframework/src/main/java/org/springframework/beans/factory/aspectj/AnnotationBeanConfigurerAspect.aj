// package org.springframework.beans.factory.aspectj;
//
// import org.aspectj.lang.annotation.control.CodeGenerationHint;
// import org.springframework.beans.factory.BeanFactory;
// import org.springframework.beans.factory.BeanFactoryAware;
// import org.springframework.beans.factory.DisposableBean;
// import org.springframework.beans.factory.InitializingBean;
// import org.springframework.beans.factory.annotation.AnnotationBeanWiringInfoResolver;
// import org.springframework.beans.factory.annotation.Configurable;
// import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
//
// public aspect AnnotationBeanConfigurerAspect extends AbstractInterfaceDrivenDependencyInjectionAspect implements BeanFactoryAware, InitializingBean, DisposableBean {
//
//     private BeanConfigurerSupport beanConfigurerSupport = new BeanConfigurerSupport();
//
//     public void setBeanFactory(BeanFactory beanFactory) {
//         this.beanConfigurerSupport.setBeanWiringInfoResolver(new AnnotationBeanWiringInfoResolver());
//         this.beanConfigurerSupport.setBeanFactory(beanFactory);
//     }
//
//     public void afterPropertiesSet() {
//         this.beanConfigurerSupport.afterPropertiesSet();
//     }
//
//     public void configureBean(Object bean) {
//         this.beanConfigurerSupport.configureBean(bean);
//     }
//
//     public void destroy() {
//         this.beanConfigurerSupport.destroy();
//     }
//
//
//     public pointcut inConfigurableBean(): @this(Configurable);
//
//     public pointcut preConstructionConfiguration(): preConstructionConfigurationSupport(*);
//
//
//     @CodeGenerationHint(ifNameSuffix = "bb0") private pointcut preConstructionConfigurationSupport(Configurable c): @this(c) && if (c.preConstruction());
//
//
//     declare parents:@Configurable*implements ConfigurableObject;
//
//
//     declare parents:@Configurable Serializable+implements ConfigurableDeserializationSupport;
//
// }
