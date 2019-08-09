// package org.springframework.beans.factory.groovy;
//
// import groovy.lang.*;
// import org.codehaus.groovy.runtime.DefaultGroovyMethods;
// import org.codehaus.groovy.runtime.InvokerHelper;
// import org.springframework.beans.MutablePropertyValues;
// import org.springframework.beans.factory.BeanDefinitionStoreException;
// import org.springframework.beans.factory.config.RuntimeBeanReference;
// import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
// import org.springframework.beans.factory.parsing.Location;
// import org.springframework.beans.factory.parsing.Problem;
// import org.springframework.beans.factory.support.*;
// import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
// import org.springframework.beans.factory.xml.NamespaceHandler;
// import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
// import org.springframework.beans.factory.xml.XmlReaderContext;
// import org.springframework.core.io.DescriptiveResource;
// import org.springframework.core.io.Resource;
// import org.springframework.core.io.support.EncodedResource;
// import org.springframework.util.ObjectUtils;
// import org.springframework.util.StringUtils;
//
// import java.io.IOException;
// import java.util.*;
//
// public class GroovyBeanDefinitionReader extends AbstractBeanDefinitionReader implements GroovyObject {
//
//     private final XmlBeanDefinitionReader standardXmlBeanDefinitionReader;
//
//     private final XmlBeanDefinitionReader groovyDslXmlBeanDefinitionReader;
//
//     private final Map<String, String> namespaces = new HashMap<>();
//
//     private final Map<String, DeferredProperty> deferredProperties = new HashMap<>();
//
//     private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
//
//     private Binding binding;
//
//     private GroovyBeanDefinitionWrapper currentBeanDefinition;
//
//     public GroovyBeanDefinitionReader(BeanDefinitionRegistry registry) {
//         super(registry);
//         this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
//         this.groovyDslXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
//         this.groovyDslXmlBeanDefinitionReader.setValidating(false);
//     }
//
//     public GroovyBeanDefinitionReader(XmlBeanDefinitionReader xmlBeanDefinitionReader) {
//         super(xmlBeanDefinitionReader.getRegistry());
//         this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(xmlBeanDefinitionReader.getRegistry());
//         this.groovyDslXmlBeanDefinitionReader = xmlBeanDefinitionReader;
//     }
//
//     public void setMetaClass(MetaClass metaClass) {
//         this.metaClass = metaClass;
//     }
//
//     public MetaClass getMetaClass() {
//         return this.metaClass;
//     }
//
//     public void setBinding(Binding binding) {
//         this.binding = binding;
//     }
//
//     public Binding getBinding() {
//         return this.binding;
//     }
//     // TRADITIONAL BEAN DEFINITION READER METHODS
//
//     public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
//         return loadBeanDefinitions(new EncodedResource(resource));
//     }
//
//     @SuppressWarnings("rawtypes")
//     public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
//         // Check for XML files and redirect them to the "standard" XmlBeanDefinitionReader
//         String filename = encodedResource.getResource().getFilename();
//         if (StringUtils.endsWithIgnoreCase(filename, ".xml")) {
//             return this.standardXmlBeanDefinitionReader.loadBeanDefinitions(encodedResource);
//         }
//         if (logger.isTraceEnabled()) {
//             logger.trace("Loading Groovy bean definitions from " + encodedResource);
//         }
//         Closure beans = new Closure(this) {
//             @Override
//             public Object call(Object... args) {
//                 invokeBeanDefiningClosure((Closure) args[0]);
//                 return null;
//             }
//         };
//         Binding binding = new Binding() {
//             @Override
//             public void setVariable(String name, Object value) {
//                 if (currentBeanDefinition != null) {
//                     applyPropertyToBeanDefinition(name, value);
//                 } else {
//                     super.setVariable(name, value);
//                 }
//             }
//         };
//         binding.setVariable("beans", beans);
//         int countBefore = getRegistry().getBeanDefinitionCount();
//         try {
//             GroovyShell shell = new GroovyShell(getBeanClassLoader(), binding);
//             shell.evaluate(encodedResource.getReader(), "beans");
//         } catch (Throwable ex) {
//             throw new BeanDefinitionParsingException(new Problem("Error evaluating Groovy script: " + ex.getMessage(), new Location(encodedResource.getResource()), null, ex));
//         }
//         int count = getRegistry().getBeanDefinitionCount() - countBefore;
//         if (logger.isDebugEnabled()) {
//             logger.debug("Loaded " + count + " bean definitions from " + encodedResource);
//         }
//         return count;
//     }
//     // METHODS FOR CONSUMPTION IN A GROOVY CLOSURE
//
//     @SuppressWarnings("rawtypes")
//     public GroovyBeanDefinitionReader beans(Closure closure) {
//         return invokeBeanDefiningClosure(closure);
//     }
//
//     public GenericBeanDefinition bean(Class<?> type) {
//         GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
//         beanDefinition.setBeanClass(type);
//         return beanDefinition;
//     }
//
//     @SuppressWarnings("rawtypes")
//     public AbstractBeanDefinition bean(Class<?> type, Object... args) {
//         GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
//         try {
//             Closure callable = null;
//             Collection constructorArgs = null;
//             if (!ObjectUtils.isEmpty(args)) {
//                 int index = args.length;
//                 Object lastArg = args[index - 1];
//                 if (lastArg instanceof Closure) {
//                     callable = (Closure) lastArg;
//                     index--;
//                 }
//                 if (index > -1) {
//                     constructorArgs = resolveConstructorArguments(args, 0, index);
//                 }
//             }
//             this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, type, constructorArgs);
//             if (callable != null) {
//                 callable.call(this.currentBeanDefinition);
//             }
//             return this.currentBeanDefinition.getBeanDefinition();
//
//         } finally {
//             this.currentBeanDefinition = current;
//         }
//     }
//
//     public void xmlns(Map<String, String> definition) {
//         if (!definition.isEmpty()) {
//             for (Map.Entry<String, String> entry : definition.entrySet()) {
//                 String namespace = entry.getKey();
//                 String uri = entry.getValue();
//                 if (uri == null) {
//                     throw new IllegalArgumentException("Namespace definition must supply a non-null URI");
//                 }
//                 NamespaceHandler namespaceHandler = this.groovyDslXmlBeanDefinitionReader.getNamespaceHandlerResolver().resolve(uri);
//                 if (namespaceHandler == null) {
//                     throw new BeanDefinitionParsingException(new Problem("No namespace handler found for URI: " + uri, new Location(new DescriptiveResource(("Groovy")))));
//                 }
//                 this.namespaces.put(namespace, uri);
//             }
//         }
//     }
//
//     public void importBeans(String resourcePattern) throws IOException {
//         loadBeanDefinitions(resourcePattern);
//     }
//     // INTERNAL HANDLING OF GROOVY CLOSURES AND PROPERTIES
//
//     @SuppressWarnings("rawtypes")
//     public Object invokeMethod(String name, Object arg) {
//         Object[] args = (Object[]) arg;
//         if ("beans".equals(name) && args.length == 1 && args[0] instanceof Closure) {
//             return beans((Closure) args[0]);
//         } else if ("ref".equals(name)) {
//             String refName;
//             if (args[0] == null) {
//                 throw new IllegalArgumentException("Argument to ref() is not a valid bean or was not found");
//             }
//             if (args[0] instanceof RuntimeBeanReference) {
//                 refName = ((RuntimeBeanReference) args[0]).getBeanName();
//             } else {
//                 refName = args[0].toString();
//             }
//             boolean parentRef = false;
//             if (args.length > 1 && args[1] instanceof Boolean) {
//                 parentRef = (Boolean) args[1];
//             }
//             return new RuntimeBeanReference(refName, parentRef);
//         } else if (this.namespaces.containsKey(name) && args.length > 0 && args[0] instanceof Closure) {
//             GroovyDynamicElementReader reader = createDynamicElementReader(name);
//             reader.invokeMethod("doCall", args);
//         } else if (args.length > 0 && args[0] instanceof Closure) {
//             // abstract bean definition
//             return invokeBeanDefiningMethod(name, args);
//         } else if (args.length > 0 && (args[0] instanceof Class || args[0] instanceof RuntimeBeanReference || args[0] instanceof Map)) {
//             return invokeBeanDefiningMethod(name, args);
//         } else if (args.length > 1 && args[args.length - 1] instanceof Closure) {
//             return invokeBeanDefiningMethod(name, args);
//         }
//         MetaClass mc = DefaultGroovyMethods.getMetaClass(getRegistry());
//         if (!mc.respondsTo(getRegistry(), name, args).isEmpty()) {
//             return mc.invokeMethod(getRegistry(), name, args);
//         }
//         return this;
//     }
//
//     private boolean addDeferredProperty(String property, Object newValue) {
//         if (newValue instanceof List || newValue instanceof Map) {
//             this.deferredProperties.put(this.currentBeanDefinition.getBeanName() + '.' + property, new DeferredProperty(this.currentBeanDefinition, property, newValue));
//             return true;
//         }
//         return false;
//     }
//
//     @SuppressWarnings("rawtypes")
//     private void finalizeDeferredProperties() {
//         for (DeferredProperty dp : this.deferredProperties.values()) {
//             if (dp.value instanceof List) {
//                 dp.value = manageListIfNecessary((List) dp.value);
//             } else if (dp.value instanceof Map) {
//                 dp.value = manageMapIfNecessary((Map) dp.value);
//             }
//             dp.apply();
//         }
//         this.deferredProperties.clear();
//     }
//
//     @SuppressWarnings("rawtypes")
//     protected GroovyBeanDefinitionReader invokeBeanDefiningClosure(Closure callable) {
//         callable.setDelegate(this);
//         callable.call();
//         finalizeDeferredProperties();
//         return this;
//     }
//
//     @SuppressWarnings("rawtypes")
//     private GroovyBeanDefinitionWrapper invokeBeanDefiningMethod(String beanName, Object[] args) {
//         boolean hasClosureArgument = (args[args.length - 1] instanceof Closure);
//         if (args[0] instanceof Class) {
//             Class<?> beanClass = (Class<?>) args[0];
//             if (hasClosureArgument) {
//                 if (args.length - 1 != 1) {
//                     this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, beanClass, resolveConstructorArguments(args, 1, args.length - 1));
//                 } else {
//                     this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, beanClass);
//                 }
//             } else {
//                 this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, beanClass, resolveConstructorArguments(args, 1, args.length));
//             }
//         } else if (args[0] instanceof RuntimeBeanReference) {
//             this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
//             this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(((RuntimeBeanReference) args[0]).getBeanName());
//         } else if (args[0] instanceof Map) {
//             // named constructor arguments
//             if (args.length > 1 && args[1] instanceof Class) {
//                 List<Object> constructorArgs = resolveConstructorArguments(args, 2, hasClosureArgument ? args.length - 1 : args.length);
//                 this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, (Class<?>) args[1], constructorArgs);
//                 Map namedArgs = (Map) args[0];
//                 for (Object o : namedArgs.keySet()) {
//                     String propName = (String) o;
//                     setProperty(propName, namedArgs.get(propName));
//                 }
//             }
//             // factory method syntax
//             else {
//                 this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
//                 // First arg is the map containing factoryBean : factoryMethod
//                 Map.Entry factoryBeanEntry = (Map.Entry) ((Map) args[0]).entrySet().iterator().next();
//                 // If we have a closure body, that will be the last argument.
//                 // In between are the constructor args
//                 int constructorArgsTest = (hasClosureArgument ? 2 : 1);
//                 // If we have more than this number of args, we have constructor args
//                 if (args.length > constructorArgsTest) {
//                     // factory-method requires args
//                     int endOfConstructArgs = (hasClosureArgument ? args.length - 1 : args.length);
//                     this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null, resolveConstructorArguments(args, 1, endOfConstructArgs));
//                 } else {
//                     this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
//                 }
//                 this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(factoryBeanEntry.getKey().toString());
//                 this.currentBeanDefinition.getBeanDefinition().setFactoryMethodName(factoryBeanEntry.getValue().toString());
//             }
//
//         } else if (args[0] instanceof Closure) {
//             this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
//             this.currentBeanDefinition.getBeanDefinition().setAbstract(true);
//         } else {
//             List constructorArgs = resolveConstructorArguments(args, 0, hasClosureArgument ? args.length - 1 : args.length);
//             this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null, constructorArgs);
//         }
//         if (hasClosureArgument) {
//             Closure callable = (Closure) args[args.length - 1];
//             callable.setDelegate(this);
//             callable.setResolveStrategy(Closure.DELEGATE_FIRST);
//             callable.call(this.currentBeanDefinition);
//         }
//         GroovyBeanDefinitionWrapper beanDefinition = this.currentBeanDefinition;
//         this.currentBeanDefinition = null;
//         beanDefinition.getBeanDefinition().setAttribute(GroovyBeanDefinitionWrapper.class.getName(), beanDefinition);
//         getRegistry().registerBeanDefinition(beanName, beanDefinition.getBeanDefinition());
//         return beanDefinition;
//     }
//
//     @SuppressWarnings("rawtypes")
//     protected List<Object> resolveConstructorArguments(Object[] args, int start, int end) {
//         Object[] constructorArgs = Arrays.copyOfRange(args, start, end);
//         for (int i = 0; i < constructorArgs.length; i++) {
//             if (constructorArgs[i] instanceof GString) {
//                 constructorArgs[i] = constructorArgs[i].toString();
//             } else if (constructorArgs[i] instanceof List) {
//                 constructorArgs[i] = manageListIfNecessary((List) constructorArgs[i]);
//             } else if (constructorArgs[i] instanceof Map) {
//                 constructorArgs[i] = manageMapIfNecessary((Map) constructorArgs[i]);
//             }
//         }
//         return Arrays.asList(constructorArgs);
//     }
//
//     private Object manageMapIfNecessary(Map<?, ?> map) {
//         boolean containsRuntimeRefs = false;
//         for (Object element : map.values()) {
//             if (element instanceof RuntimeBeanReference) {
//                 containsRuntimeRefs = true;
//                 break;
//             }
//         }
//         if (containsRuntimeRefs) {
//             Map<Object, Object> managedMap = new ManagedMap<>();
//             managedMap.putAll(map);
//             return managedMap;
//         }
//         return map;
//     }
//
//     private Object manageListIfNecessary(List<?> list) {
//         boolean containsRuntimeRefs = false;
//         for (Object element : list) {
//             if (element instanceof RuntimeBeanReference) {
//                 containsRuntimeRefs = true;
//                 break;
//             }
//         }
//         if (containsRuntimeRefs) {
//             List<Object> managedList = new ManagedList<>();
//             managedList.addAll(list);
//             return managedList;
//         }
//         return list;
//     }
//
//     public void setProperty(String name, Object value) {
//         if (this.currentBeanDefinition != null) {
//             applyPropertyToBeanDefinition(name, value);
//         }
//     }
//
//     @SuppressWarnings("rawtypes")
//     protected void applyPropertyToBeanDefinition(String name, Object value) {
//         if (value instanceof GString) {
//             value = value.toString();
//         }
//         if (addDeferredProperty(name, value)) {
//             return;
//         } else if (value instanceof Closure) {
//             GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
//             try {
//                 Closure callable = (Closure) value;
//                 Class<?> parameterType = callable.getParameterTypes()[0];
//                 if (Object.class == parameterType) {
//                     this.currentBeanDefinition = new GroovyBeanDefinitionWrapper("");
//                     callable.call(this.currentBeanDefinition);
//                 } else {
//                     this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, parameterType);
//                     callable.call((Object) null);
//                 }
//                 value = this.currentBeanDefinition.getBeanDefinition();
//             } finally {
//                 this.currentBeanDefinition = current;
//             }
//         }
//         this.currentBeanDefinition.addProperty(name, value);
//     }
//
//     public Object getProperty(String name) {
//         Binding binding = getBinding();
//         if (binding != null && binding.hasVariable(name)) {
//             return binding.getVariable(name);
//         } else {
//             if (this.namespaces.containsKey(name)) {
//                 return createDynamicElementReader(name);
//             }
//             if (getRegistry().containsBeanDefinition(name)) {
//                 GroovyBeanDefinitionWrapper beanDefinition = (GroovyBeanDefinitionWrapper) getRegistry().getBeanDefinition(name).getAttribute(GroovyBeanDefinitionWrapper.class.getName());
//                 if (beanDefinition != null) {
//                     return new GroovyRuntimeBeanReference(name, beanDefinition, false);
//                 } else {
//                     return new RuntimeBeanReference(name, false);
//                 }
//             }
//             // This is to deal with the case where the property setter is the last
//             // statement in a closure (hence the return value)
//             else if (this.currentBeanDefinition != null) {
//                 MutablePropertyValues pvs = this.currentBeanDefinition.getBeanDefinition().getPropertyValues();
//                 if (pvs.contains(name)) {
//                     return pvs.get(name);
//                 } else {
//                     DeferredProperty dp = this.deferredProperties.get(this.currentBeanDefinition.getBeanName() + name);
//                     if (dp != null) {
//                         return dp.value;
//                     } else {
//                         return getMetaClass().getProperty(this, name);
//                     }
//                 }
//             } else {
//                 return getMetaClass().getProperty(this, name);
//             }
//         }
//     }
//
//     private GroovyDynamicElementReader createDynamicElementReader(String namespace) {
//         XmlReaderContext readerContext = this.groovyDslXmlBeanDefinitionReader.createReaderContext(new DescriptiveResource("Groovy"));
//         BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
//         boolean decorating = (this.currentBeanDefinition != null);
//         if (!decorating) {
//             this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(namespace);
//         }
//         return new GroovyDynamicElementReader(namespace, this.namespaces, delegate, this.currentBeanDefinition, decorating) {
//             @Override
//             protected void afterInvocation() {
//                 if (!this.decorating) {
//                     currentBeanDefinition = null;
//                 }
//             }
//         };
//     }
//
//     private static class DeferredProperty {
//
//         private final GroovyBeanDefinitionWrapper beanDefinition;
//
//         private final String name;
//
//         public Object value;
//
//         public DeferredProperty(GroovyBeanDefinitionWrapper beanDefinition, String name, Object value) {
//             this.beanDefinition = beanDefinition;
//             this.name = name;
//             this.value = value;
//         }
//
//         public void apply() {
//             this.beanDefinition.addProperty(this.name, this.value);
//         }
//
//     }
//
//     private class GroovyRuntimeBeanReference extends RuntimeBeanReference implements GroovyObject {
//
//         private final GroovyBeanDefinitionWrapper beanDefinition;
//
//         private MetaClass metaClass;
//
//         public GroovyRuntimeBeanReference(String beanName, GroovyBeanDefinitionWrapper beanDefinition, boolean toParent) {
//             super(beanName, toParent);
//             this.beanDefinition = beanDefinition;
//             this.metaClass = InvokerHelper.getMetaClass(this);
//         }
//
//         public MetaClass getMetaClass() {
//             return this.metaClass;
//         }
//
//         public Object getProperty(String property) {
//             if (property.equals("beanName")) {
//                 return getBeanName();
//             } else if (property.equals("source")) {
//                 return getSource();
//             } else if (this.beanDefinition != null) {
//                 return new GroovyPropertyValue(property, this.beanDefinition.getBeanDefinition().getPropertyValues().get(property));
//             } else {
//                 return this.metaClass.getProperty(this, property);
//             }
//         }
//
//         public Object invokeMethod(String name, Object args) {
//             return this.metaClass.invokeMethod(this, name, args);
//         }
//
//         public void setMetaClass(MetaClass metaClass) {
//             this.metaClass = metaClass;
//         }
//
//         public void setProperty(String property, Object newValue) {
//             if (!addDeferredProperty(property, newValue)) {
//                 this.beanDefinition.getBeanDefinition().getPropertyValues().add(property, newValue);
//             }
//         }
//
//         private class GroovyPropertyValue extends GroovyObjectSupport {
//
//             private final String propertyName;
//
//             private final Object propertyValue;
//
//             public GroovyPropertyValue(String propertyName, Object propertyValue) {
//                 this.propertyName = propertyName;
//                 this.propertyValue = propertyValue;
//             }
//
//             @SuppressWarnings("unused")
//             public void leftShift(Object value) {
//                 InvokerHelper.invokeMethod(this.propertyValue, "leftShift", value);
//                 updateDeferredProperties(value);
//             }
//
//             @SuppressWarnings("unused")
//             public boolean add(Object value) {
//                 boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "add", value);
//                 updateDeferredProperties(value);
//                 return retVal;
//             }
//
//             @SuppressWarnings({"rawtypes", "unused"})
//             public boolean addAll(Collection values) {
//                 boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "addAll", values);
//                 for (Object value : values) {
//                     updateDeferredProperties(value);
//                 }
//                 return retVal;
//             }
//
//             @Override
//             public Object invokeMethod(String name, Object args) {
//                 return InvokerHelper.invokeMethod(this.propertyValue, name, args);
//             }
//
//             @Override
//             public Object getProperty(String name) {
//                 return InvokerHelper.getProperty(this.propertyValue, name);
//             }
//
//             @Override
//             public void setProperty(String name, Object value) {
//                 InvokerHelper.setProperty(this.propertyValue, name, value);
//             }
//
//             private void updateDeferredProperties(Object value) {
//                 if (value instanceof RuntimeBeanReference) {
//                     deferredProperties.put(beanDefinition.getBeanName(), new DeferredProperty(beanDefinition, this.propertyName, this.propertyValue));
//                 }
//             }
//
//         }
//
//     }
//
// }
