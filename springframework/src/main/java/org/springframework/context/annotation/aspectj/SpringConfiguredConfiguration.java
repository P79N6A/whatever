// package org.springframework.context.annotation.aspectj;
//
// import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
// import org.springframework.beans.factory.config.BeanDefinition;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Role;
//
// @Configuration
// public class SpringConfiguredConfiguration {
//
//     public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME = "org.springframework.context.config.internalBeanConfigurerAspect";
//
//     @Bean(name = BEAN_CONFIGURER_ASPECT_BEAN_NAME)
//     @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
//     public AnnotationBeanConfigurerAspect beanConfigurerAspect() {
//         return AnnotationBeanConfigurerAspect.aspectOf();
//     }
//
// }
