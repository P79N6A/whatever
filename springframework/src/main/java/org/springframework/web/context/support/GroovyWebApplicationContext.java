// package org.springframework.web.context.support;
//
// import groovy.lang.GroovyObject;
// import groovy.lang.GroovySystem;
// import groovy.lang.MetaClass;
// import org.springframework.beans.BeanWrapper;
// import org.springframework.beans.BeanWrapperImpl;
// import org.springframework.beans.BeansException;
// import org.springframework.beans.factory.NoSuchBeanDefinitionException;
// import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
// import org.springframework.beans.factory.support.DefaultListableBeanFactory;
// import org.springframework.lang.Nullable;
//
// import java.io.IOException;
//
// public class GroovyWebApplicationContext extends AbstractRefreshableWebApplicationContext implements GroovyObject {
//
//     public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.groovy";
//
//     public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";
//
//     public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".groovy";
//
//     private final BeanWrapper contextWrapper = new BeanWrapperImpl(this);
//
//     private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
//
//     @Override
//     protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
//         // Create a new XmlBeanDefinitionReader for the given BeanFactory.
//         GroovyBeanDefinitionReader beanDefinitionReader = new GroovyBeanDefinitionReader(beanFactory);
//         // Configure the bean definition reader with this context's
//         // resource loading environment.
//         beanDefinitionReader.setEnvironment(getEnvironment());
//         beanDefinitionReader.setResourceLoader(this);
//         // Allow a subclass to provide custom initialization of the reader,
//         // then proceed with actually loading the bean definitions.
//         initBeanDefinitionReader(beanDefinitionReader);
//         loadBeanDefinitions(beanDefinitionReader);
//     }
//
//     protected void initBeanDefinitionReader(GroovyBeanDefinitionReader beanDefinitionReader) {
//     }
//
//     protected void loadBeanDefinitions(GroovyBeanDefinitionReader reader) throws IOException {
//         String[] configLocations = getConfigLocations();
//         if (configLocations != null) {
//             for (String configLocation : configLocations) {
//                 reader.loadBeanDefinitions(configLocation);
//             }
//         }
//     }
//
//     @Override
//     protected String[] getDefaultConfigLocations() {
//         if (getNamespace() != null) {
//             return new String[]{DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace() + DEFAULT_CONFIG_LOCATION_SUFFIX};
//         } else {
//             return new String[]{DEFAULT_CONFIG_LOCATION};
//         }
//     }
//     // Implementation of the GroovyObject interface
//
//     public void setMetaClass(MetaClass metaClass) {
//         this.metaClass = metaClass;
//     }
//
//     public MetaClass getMetaClass() {
//         return this.metaClass;
//     }
//
//     public Object invokeMethod(String name, Object args) {
//         return this.metaClass.invokeMethod(this, name, args);
//     }
//
//     public void setProperty(String property, Object newValue) {
//         this.metaClass.setProperty(this, property, newValue);
//     }
//
//     @Nullable
//     public Object getProperty(String property) {
//         if (containsBean(property)) {
//             return getBean(property);
//         } else if (this.contextWrapper.isReadableProperty(property)) {
//             return this.contextWrapper.getPropertyValue(property);
//         }
//         throw new NoSuchBeanDefinitionException(property);
//     }
//
// }
