package org.springframework.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CachedIntrospectionResults {

    public static final String IGNORE_BEANINFO_PROPERTY_NAME = "spring.beaninfo.ignore";

    private static final boolean shouldIntrospectorIgnoreBeaninfoClasses = SpringProperties.getFlag(IGNORE_BEANINFO_PROPERTY_NAME);

    private static List<BeanInfoFactory> beanInfoFactories = SpringFactoriesLoader.loadFactories(BeanInfoFactory.class, CachedIntrospectionResults.class.getClassLoader());

    private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);

    static final Set<ClassLoader> acceptedClassLoaders = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache = new ConcurrentHashMap<>(64);

    static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache = new ConcurrentReferenceHashMap<>(64);

    public static void acceptClassLoader(@Nullable ClassLoader classLoader) {
        if (classLoader != null) {
            acceptedClassLoaders.add(classLoader);
        }
    }

    public static void clearClassLoader(@Nullable ClassLoader classLoader) {
        acceptedClassLoaders.removeIf(registeredLoader -> isUnderneathClassLoader(registeredLoader, classLoader));
        strongClassCache.keySet().removeIf(beanClass -> isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
        softClassCache.keySet().removeIf(beanClass -> isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
    }

    @SuppressWarnings("unchecked")
    static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
        CachedIntrospectionResults results = strongClassCache.get(beanClass);
        if (results != null) {
            return results;
        }
        results = softClassCache.get(beanClass);
        if (results != null) {
            return results;
        }
        results = new CachedIntrospectionResults(beanClass);
        ConcurrentMap<Class<?>, CachedIntrospectionResults> classCacheToUse;
        if (ClassUtils.isCacheSafe(beanClass, CachedIntrospectionResults.class.getClassLoader()) || isClassLoaderAccepted(beanClass.getClassLoader())) {
            classCacheToUse = strongClassCache;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Not strongly caching class [" + beanClass.getName() + "] because it is not cache-safe");
            }
            classCacheToUse = softClassCache;
        }
        CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
        return (existing != null ? existing : results);
    }

    private static boolean isClassLoaderAccepted(ClassLoader classLoader) {
        for (ClassLoader acceptedLoader : acceptedClassLoaders) {
            if (isUnderneathClassLoader(classLoader, acceptedLoader)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnderneathClassLoader(@Nullable ClassLoader candidate, @Nullable ClassLoader parent) {
        if (candidate == parent) {
            return true;
        }
        if (candidate == null) {
            return false;
        }
        ClassLoader classLoaderToCheck = candidate;
        while (classLoaderToCheck != null) {
            classLoaderToCheck = classLoaderToCheck.getParent();
            if (classLoaderToCheck == parent) {
                return true;
            }
        }
        return false;
    }

    private static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        for (BeanInfoFactory beanInfoFactory : beanInfoFactories) {
            BeanInfo beanInfo = beanInfoFactory.getBeanInfo(beanClass);
            if (beanInfo != null) {
                return beanInfo;
            }
        }
        return (shouldIntrospectorIgnoreBeaninfoClasses ? Introspector.getBeanInfo(beanClass, Introspector.IGNORE_ALL_BEANINFO) : Introspector.getBeanInfo(beanClass));
    }

    private final BeanInfo beanInfo;

    private final Map<String, PropertyDescriptor> propertyDescriptorCache;

    private final ConcurrentMap<PropertyDescriptor, TypeDescriptor> typeDescriptorCache;

    private CachedIntrospectionResults(Class<?> beanClass) throws BeansException {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting BeanInfo for class [" + beanClass.getName() + "]");
            }
            this.beanInfo = getBeanInfo(beanClass);
            if (logger.isTraceEnabled()) {
                logger.trace("Caching PropertyDescriptors for class [" + beanClass.getName() + "]");
            }
            this.propertyDescriptorCache = new LinkedHashMap<>();
            // This call is slow so we do it once.
            PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (Class.class == beanClass && ("classLoader".equals(pd.getName()) || "protectionDomain".equals(pd.getName()))) {
                    // Ignore Class.getClassLoader() and getProtectionDomain() methods - nobody needs to bind to those
                    continue;
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Found bean property '" + pd.getName() + "'" + (pd.getPropertyType() != null ? " of type [" + pd.getPropertyType().getName() + "]" : "") + (pd.getPropertyEditorClass() != null ? "; editor [" + pd.getPropertyEditorClass().getName() + "]" : ""));
                }
                pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
                this.propertyDescriptorCache.put(pd.getName(), pd);
            }
            // Explicitly check implemented interfaces for setter/getter methods as well,
            // in particular for Java 8 default methods...
            Class<?> currClass = beanClass;
            while (currClass != null && currClass != Object.class) {
                introspectInterfaces(beanClass, currClass);
                currClass = currClass.getSuperclass();
            }
            this.typeDescriptorCache = new ConcurrentReferenceHashMap<>();
        } catch (IntrospectionException ex) {
            throw new FatalBeanException("Failed to obtain BeanInfo for class [" + beanClass.getName() + "]", ex);
        }
    }

    private void introspectInterfaces(Class<?> beanClass, Class<?> currClass) throws IntrospectionException {
        for (Class<?> ifc : currClass.getInterfaces()) {
            if (!ClassUtils.isJavaLanguageInterface(ifc)) {
                for (PropertyDescriptor pd : getBeanInfo(ifc).getPropertyDescriptors()) {
                    PropertyDescriptor existingPd = this.propertyDescriptorCache.get(pd.getName());
                    if (existingPd == null || (existingPd.getReadMethod() == null && pd.getReadMethod() != null)) {
                        // GenericTypeAwarePropertyDescriptor leniently resolves a set* write method
                        // against a declared read method, so we prefer read method descriptors here.
                        pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
                        this.propertyDescriptorCache.put(pd.getName(), pd);
                    }
                }
                introspectInterfaces(ifc, ifc);
            }
        }
    }

    BeanInfo getBeanInfo() {
        return this.beanInfo;
    }

    Class<?> getBeanClass() {
        return this.beanInfo.getBeanDescriptor().getBeanClass();
    }

    @Nullable
    PropertyDescriptor getPropertyDescriptor(String name) {
        PropertyDescriptor pd = this.propertyDescriptorCache.get(name);
        if (pd == null && StringUtils.hasLength(name)) {
            // Same lenient fallback checking as in Property...
            pd = this.propertyDescriptorCache.get(StringUtils.uncapitalize(name));
            if (pd == null) {
                pd = this.propertyDescriptorCache.get(StringUtils.capitalize(name));
            }
        }
        return (pd == null || pd instanceof GenericTypeAwarePropertyDescriptor ? pd : buildGenericTypeAwarePropertyDescriptor(getBeanClass(), pd));
    }

    PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] pds = new PropertyDescriptor[this.propertyDescriptorCache.size()];
        int i = 0;
        for (PropertyDescriptor pd : this.propertyDescriptorCache.values()) {
            pds[i] = (pd instanceof GenericTypeAwarePropertyDescriptor ? pd : buildGenericTypeAwarePropertyDescriptor(getBeanClass(), pd));
            i++;
        }
        return pds;
    }

    private PropertyDescriptor buildGenericTypeAwarePropertyDescriptor(Class<?> beanClass, PropertyDescriptor pd) {
        try {
            return new GenericTypeAwarePropertyDescriptor(beanClass, pd.getName(), pd.getReadMethod(), pd.getWriteMethod(), pd.getPropertyEditorClass());
        } catch (IntrospectionException ex) {
            throw new FatalBeanException("Failed to re-introspect class [" + beanClass.getName() + "]", ex);
        }
    }

    TypeDescriptor addTypeDescriptor(PropertyDescriptor pd, TypeDescriptor td) {
        TypeDescriptor existing = this.typeDescriptorCache.putIfAbsent(pd, td);
        return (existing != null ? existing : td);
    }

    @Nullable
    TypeDescriptor getTypeDescriptor(PropertyDescriptor pd) {
        return this.typeDescriptorCache.get(pd);
    }

}
