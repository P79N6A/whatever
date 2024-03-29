package org.springframework.context.annotation;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jndi.support.SimpleJndiBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("serial")
public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, Serializable {

    @Nullable
    private static Class<? extends Annotation> webServiceRefClass;

    @Nullable
    private static Class<? extends Annotation> ejbRefClass;

    private static Set<Class<? extends Annotation>> resourceAnnotationTypes = new LinkedHashSet<>(4);

    static {
        try {
            @SuppressWarnings("unchecked") Class<? extends Annotation> clazz = (Class<? extends Annotation>) ClassUtils.forName("javax.xml.ws.WebServiceRef", CommonAnnotationBeanPostProcessor.class.getClassLoader());
            webServiceRefClass = clazz;
        } catch (ClassNotFoundException ex) {
            webServiceRefClass = null;
        }
        try {
            @SuppressWarnings("unchecked") Class<? extends Annotation> clazz = (Class<? extends Annotation>) ClassUtils.forName("javax.ejb.EJB", CommonAnnotationBeanPostProcessor.class.getClassLoader());
            ejbRefClass = clazz;
        } catch (ClassNotFoundException ex) {
            ejbRefClass = null;
        }
        resourceAnnotationTypes.add(Resource.class);
        if (webServiceRefClass != null) {
            resourceAnnotationTypes.add(webServiceRefClass);
        }
        if (ejbRefClass != null) {
            resourceAnnotationTypes.add(ejbRefClass);
        }
    }

    private final Set<String> ignoredResourceTypes = new HashSet<>(1);

    private boolean fallbackToDefaultTypeMatch = true;

    private boolean alwaysUseJndiLookup = false;

    private transient BeanFactory jndiFactory = new SimpleJndiBeanFactory();

    @Nullable
    private transient BeanFactory resourceFactory;

    @Nullable
    private transient BeanFactory beanFactory;

    @Nullable
    private transient StringValueResolver embeddedValueResolver;

    private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

    public CommonAnnotationBeanPostProcessor() {
        setOrder(Ordered.LOWEST_PRECEDENCE - 3);
        setInitAnnotationType(PostConstruct.class);
        setDestroyAnnotationType(PreDestroy.class);
        ignoreResourceType("javax.xml.ws.WebServiceContext");
    }

    public void ignoreResourceType(String resourceType) {
        Assert.notNull(resourceType, "Ignored resource type must not be null");
        this.ignoredResourceTypes.add(resourceType);
    }

    public void setFallbackToDefaultTypeMatch(boolean fallbackToDefaultTypeMatch) {
        this.fallbackToDefaultTypeMatch = fallbackToDefaultTypeMatch;
    }

    public void setAlwaysUseJndiLookup(boolean alwaysUseJndiLookup) {
        this.alwaysUseJndiLookup = alwaysUseJndiLookup;
    }

    public void setJndiFactory(BeanFactory jndiFactory) {
        Assert.notNull(jndiFactory, "BeanFactory must not be null");
        this.jndiFactory = jndiFactory;
    }

    public void setResourceFactory(BeanFactory resourceFactory) {
        Assert.notNull(resourceFactory, "BeanFactory must not be null");
        this.resourceFactory = resourceFactory;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
        if (this.resourceFactory == null) {
            this.resourceFactory = beanFactory;
        }
        if (beanFactory instanceof ConfigurableBeanFactory) {
            this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
        }
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        super.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
        InjectionMetadata metadata = findResourceMetadata(beanName, beanType, null);
        metadata.checkConfigMembers(beanDefinition);
    }

    @Override
    public void resetBeanDefinition(String beanName) {
        this.injectionMetadataCache.remove(beanName);
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
        }
        return pvs;
    }

    @Deprecated
    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {
        return postProcessProperties(pvs, bean, beanName);
    }

    private InjectionMetadata findResourceMetadata(String beanName, final Class<?> clazz, @Nullable PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    metadata = buildResourceMetadata(clazz);
                    this.injectionMetadataCache.put(cacheKey, metadata);
                }
            }
        }
        return metadata;
    }

    private InjectionMetadata buildResourceMetadata(final Class<?> clazz) {
        if (!AnnotationUtils.isCandidateClass(clazz, resourceAnnotationTypes)) {
            return InjectionMetadata.EMPTY;
        }
        List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
        Class<?> targetClass = clazz;
        do {
            final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                if (webServiceRefClass != null && field.isAnnotationPresent(webServiceRefClass)) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        throw new IllegalStateException("@WebServiceRef annotation is not supported on static fields");
                    }
                    currElements.add(new WebServiceRefElement(field, field, null));
                } else if (ejbRefClass != null && field.isAnnotationPresent(ejbRefClass)) {
                    // if (Modifier.isStatic(field.getModifiers())) {
                    //     throw new IllegalStateException("@EJB annotation is not supported on static fields");
                    // }
                    // currElements.add(new EjbRefElement(field, field, null));
                } else if (field.isAnnotationPresent(Resource.class)) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        throw new IllegalStateException("@Resource annotation is not supported on static fields");
                    }
                    if (!this.ignoredResourceTypes.contains(field.getType().getName())) {
                        currElements.add(new ResourceElement(field, field, null));
                    }
                }
            });
            ReflectionUtils.doWithLocalMethods(targetClass, method -> {
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
                if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                    return;
                }
                if (method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
                    if (webServiceRefClass != null && bridgedMethod.isAnnotationPresent(webServiceRefClass)) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalStateException("@WebServiceRef annotation is not supported on static methods");
                        }
                        if (method.getParameterCount() != 1) {
                            throw new IllegalStateException("@WebServiceRef annotation requires a single-arg method: " + method);
                        }
                        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                        currElements.add(new WebServiceRefElement(method, bridgedMethod, pd));
                    } else if (ejbRefClass != null && bridgedMethod.isAnnotationPresent(ejbRefClass)) {
                        // if (Modifier.isStatic(method.getModifiers())) {
                        //     throw new IllegalStateException("@EJB annotation is not supported on static methods");
                        // }
                        // if (method.getParameterCount() != 1) {
                        //     throw new IllegalStateException("@EJB annotation requires a single-arg method: " + method);
                        // }
                        // PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                        // currElements.add(new EjbRefElement(method, bridgedMethod, pd));
                    } else if (bridgedMethod.isAnnotationPresent(Resource.class)) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalStateException("@Resource annotation is not supported on static methods");
                        }
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length != 1) {
                            throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
                        }
                        if (!this.ignoredResourceTypes.contains(paramTypes[0].getName())) {
                            PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                            currElements.add(new ResourceElement(method, bridgedMethod, pd));
                        }
                    }
                }
            });
            elements.addAll(0, currElements);
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);
        return InjectionMetadata.forElements(elements, clazz);
    }

    protected Object buildLazyResourceProxy(final LookupElement element, final @Nullable String requestingBeanName) {
        TargetSource ts = new TargetSource() {
            @Override
            public Class<?> getTargetClass() {
                return element.lookupType;
            }

            @Override
            public boolean isStatic() {
                return false;
            }

            @Override
            public Object getTarget() {
                return getResource(element, requestingBeanName);
            }

            @Override
            public void releaseTarget(Object target) {
            }
        };
        ProxyFactory pf = new ProxyFactory();
        pf.setTargetSource(ts);
        if (element.lookupType.isInterface()) {
            pf.addInterface(element.lookupType);
        }
        ClassLoader classLoader = (this.beanFactory instanceof ConfigurableBeanFactory ? ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() : null);
        return pf.getProxy(classLoader);
    }

    protected Object getResource(LookupElement element, @Nullable String requestingBeanName) throws NoSuchBeanDefinitionException {
        if (StringUtils.hasLength(element.mappedName)) {
            return this.jndiFactory.getBean(element.mappedName, element.lookupType);
        }
        if (this.alwaysUseJndiLookup) {
            return this.jndiFactory.getBean(element.name, element.lookupType);
        }
        if (this.resourceFactory == null) {
            throw new NoSuchBeanDefinitionException(element.lookupType, "No resource factory configured - specify the 'resourceFactory' property");
        }
        return autowireResource(this.resourceFactory, element, requestingBeanName);
    }

    protected Object autowireResource(BeanFactory factory, LookupElement element, @Nullable String requestingBeanName) throws NoSuchBeanDefinitionException {
        Object resource;
        Set<String> autowiredBeanNames;
        String name = element.name;
        if (factory instanceof AutowireCapableBeanFactory) {
            AutowireCapableBeanFactory beanFactory = (AutowireCapableBeanFactory) factory;
            DependencyDescriptor descriptor = element.getDependencyDescriptor();
            if (this.fallbackToDefaultTypeMatch && element.isDefaultName && !factory.containsBean(name)) {
                autowiredBeanNames = new LinkedHashSet<>();
                resource = beanFactory.resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, null);
                if (resource == null) {
                    throw new NoSuchBeanDefinitionException(element.getLookupType(), "No resolvable resource object");
                }
            } else {
                resource = beanFactory.resolveBeanByName(name, descriptor);
                autowiredBeanNames = Collections.singleton(name);
            }
        } else {
            resource = factory.getBean(name, element.lookupType);
            autowiredBeanNames = Collections.singleton(name);
        }
        if (factory instanceof ConfigurableBeanFactory) {
            ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) factory;
            for (String autowiredBeanName : autowiredBeanNames) {
                if (requestingBeanName != null && beanFactory.containsBean(autowiredBeanName)) {
                    beanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
                }
            }
        }
        return resource;
    }

    protected abstract static class LookupElement extends InjectionMetadata.InjectedElement {

        protected String name = "";

        protected boolean isDefaultName = false;

        protected Class<?> lookupType = Object.class;

        @Nullable
        protected String mappedName;

        public LookupElement(Member member, @Nullable PropertyDescriptor pd) {
            super(member, pd);
        }

        public final String getName() {
            return this.name;
        }

        public final Class<?> getLookupType() {
            return this.lookupType;
        }

        public final DependencyDescriptor getDependencyDescriptor() {
            if (this.isField) {
                return new LookupDependencyDescriptor((Field) this.member, this.lookupType);
            } else {
                return new LookupDependencyDescriptor((Method) this.member, this.lookupType);
            }
        }

    }

    private class ResourceElement extends LookupElement {

        private final boolean lazyLookup;

        public ResourceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
            super(member, pd);
            Resource resource = ae.getAnnotation(Resource.class);
            String resourceName = resource.name();
            Class<?> resourceType = resource.type();
            this.isDefaultName = !StringUtils.hasLength(resourceName);
            if (this.isDefaultName) {
                resourceName = this.member.getName();
                if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
                    resourceName = Introspector.decapitalize(resourceName.substring(3));
                }
            } else if (embeddedValueResolver != null) {
                resourceName = embeddedValueResolver.resolveStringValue(resourceName);
            }
            if (Object.class != resourceType) {
                checkResourceType(resourceType);
            } else {
                // No resource type specified... check field/method.
                resourceType = getResourceType();
            }
            this.name = (resourceName != null ? resourceName : "");
            this.lookupType = resourceType;
            String lookupValue = resource.lookup();
            this.mappedName = (StringUtils.hasLength(lookupValue) ? lookupValue : resource.mappedName());
            Lazy lazy = ae.getAnnotation(Lazy.class);
            this.lazyLookup = (lazy != null && lazy.value());
        }

        @Override
        protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
            return (this.lazyLookup ? buildLazyResourceProxy(this, requestingBeanName) : getResource(this, requestingBeanName));
        }

    }

    private class WebServiceRefElement extends LookupElement {

        private final Class<?> elementType;

        private final String wsdlLocation;

        public WebServiceRefElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
            super(member, pd);
            WebServiceRef resource = ae.getAnnotation(WebServiceRef.class);
            String resourceName = resource.name();
            Class<?> resourceType = resource.type();
            this.isDefaultName = !StringUtils.hasLength(resourceName);
            if (this.isDefaultName) {
                resourceName = this.member.getName();
                if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
                    resourceName = Introspector.decapitalize(resourceName.substring(3));
                }
            }
            if (Object.class != resourceType) {
                checkResourceType(resourceType);
            } else {
                // No resource type specified... check field/method.
                resourceType = getResourceType();
            }
            this.name = resourceName;
            this.elementType = resourceType;
            if (Service.class.isAssignableFrom(resourceType)) {
                this.lookupType = resourceType;
            } else {
                this.lookupType = resource.value();
            }
            this.mappedName = resource.mappedName();
            this.wsdlLocation = resource.wsdlLocation();
        }

        @Override
        protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
            Service service;
            try {
                service = (Service) getResource(this, requestingBeanName);
            } catch (NoSuchBeanDefinitionException notFound) {
                // Service to be created through generated class.
                if (Service.class == this.lookupType) {
                    throw new IllegalStateException("No resource with name '" + this.name + "' found in context, " + "and no specific JAX-WS Service subclass specified. The typical solution is to either specify " + "a LocalJaxWsServiceFactoryBean with the given name or to specify the (generated) Service " + "subclass as @WebServiceRef(...) value.");
                }
                if (StringUtils.hasLength(this.wsdlLocation)) {
                    try {
                        Constructor<?> ctor = this.lookupType.getConstructor(URL.class, QName.class);
                        WebServiceClient clientAnn = this.lookupType.getAnnotation(WebServiceClient.class);
                        if (clientAnn == null) {
                            throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() + "] does not carry a WebServiceClient annotation");
                        }
                        service = (Service) BeanUtils.instantiateClass(ctor, new URL(this.wsdlLocation), new QName(clientAnn.targetNamespace(), clientAnn.name()));
                    } catch (NoSuchMethodException ex) {
                        throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() + "] does not have a (URL, QName) constructor. Cannot apply specified WSDL location [" + this.wsdlLocation + "].");
                    } catch (MalformedURLException ex) {
                        throw new IllegalArgumentException("Specified WSDL location [" + this.wsdlLocation + "] isn't a valid URL");
                    }
                } else {
                    service = (Service) BeanUtils.instantiateClass(this.lookupType);
                }
            }
            return service.getPort(this.elementType);
        }

    }
    // private class EjbRefElement extends LookupElement {
    //
    //     private final String beanName;
    //
    //     public EjbRefElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
    //         super(member, pd);
    //         EJB resource = ae.getAnnotation(EJB.class);
    //         String resourceBeanName = resource.beanName();
    //         String resourceName = resource.name();
    //         this.isDefaultName = !StringUtils.hasLength(resourceName);
    //         if (this.isDefaultName) {
    //             resourceName = this.member.getName();
    //             if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
    //                 resourceName = Introspector.decapitalize(resourceName.substring(3));
    //             }
    //         }
    //         Class<?> resourceType = resource.beanInterface();
    //         if (Object.class != resourceType) {
    //             checkResourceType(resourceType);
    //         } else {
    //             // No resource type specified... check field/method.
    //             resourceType = getResourceType();
    //         }
    //         this.beanName = resourceBeanName;
    //         this.name = resourceName;
    //         this.lookupType = resourceType;
    //         this.mappedName = resource.mappedName();
    //     }
    //
    //     @Override
    //     protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
    //         if (StringUtils.hasLength(this.beanName)) {
    //             if (beanFactory != null && beanFactory.containsBean(this.beanName)) {
    //                 // Local match found for explicitly specified local bean name.
    //                 Object bean = beanFactory.getBean(this.beanName, this.lookupType);
    //                 if (requestingBeanName != null && beanFactory instanceof ConfigurableBeanFactory) {
    //                     ((ConfigurableBeanFactory) beanFactory).registerDependentBean(this.beanName, requestingBeanName);
    //                 }
    //                 return bean;
    //             } else if (this.isDefaultName && !StringUtils.hasLength(this.mappedName)) {
    //                 throw new NoSuchBeanDefinitionException(this.beanName, "Cannot resolve 'beanName' in local BeanFactory. Consider specifying a general 'name' value instead.");
    //             }
    //         }
    //         // JNDI name lookup - may still go to a local BeanFactory.
    //         return getResource(this, requestingBeanName);
    //     }
    //
    // }

    private static class LookupDependencyDescriptor extends DependencyDescriptor {

        private final Class<?> lookupType;

        public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
            super(field, true);
            this.lookupType = lookupType;
        }

        public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
            super(new MethodParameter(method, 0), true);
            this.lookupType = lookupType;
        }

        @Override
        public Class<?> getDependencyType() {
            return this.lookupType;
        }

    }

}
