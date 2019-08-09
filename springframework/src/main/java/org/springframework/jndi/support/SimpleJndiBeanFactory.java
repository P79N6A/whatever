package org.springframework.jndi.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.core.ResolvableType;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.jndi.TypeMismatchNamingException;
import org.springframework.lang.Nullable;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.*;

public class SimpleJndiBeanFactory extends JndiLocatorSupport implements BeanFactory {

    private final Set<String> shareableResources = new HashSet<>();

    private final Map<String, Object> singletonObjects = new HashMap<>();

    private final Map<String, Class<?>> resourceTypes = new HashMap<>();

    public SimpleJndiBeanFactory() {
        setResourceRef(true);
    }

    public void addShareableResource(String shareableResource) {
        this.shareableResources.add(shareableResource);
    }

    public void setShareableResources(String... shareableResources) {
        Collections.addAll(this.shareableResources, shareableResources);
    }
    //---------------------------------------------------------------------
    // Implementation of BeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public Object getBean(String name) throws BeansException {
        return getBean(name, Object.class);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        try {
            if (isSingleton(name)) {
                return doGetSingleton(name, requiredType);
            } else {
                return lookup(name, requiredType);
            }
        } catch (NameNotFoundException ex) {
            throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
        } catch (TypeMismatchNamingException ex) {
            throw new BeanNotOfRequiredTypeException(name, ex.getRequiredType(), ex.getActualType());
        } catch (NamingException ex) {
            throw new BeanDefinitionStoreException("JNDI environment", name, "JNDI lookup failed", ex);
        }
    }

    @Override
    public Object getBean(String name, @Nullable Object... args) throws BeansException {
        if (args != null) {
            throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support explicit bean creation arguments");
        }
        return getBean(name);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return getBean(requiredType.getSimpleName(), requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
        if (args != null) {
            throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support explicit bean creation arguments");
        }
        return getBean(requiredType);
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject() throws BeansException {
                return getBean(requiredType);
            }

            @Override
            public T getObject(Object... args) throws BeansException {
                return getBean(requiredType, args);
            }

            @Override
            @Nullable
            public T getIfAvailable() throws BeansException {
                try {
                    return getBean(requiredType);
                } catch (NoUniqueBeanDefinitionException ex) {
                    throw ex;
                } catch (NoSuchBeanDefinitionException ex) {
                    return null;
                }
            }

            @Override
            @Nullable
            public T getIfUnique() throws BeansException {
                try {
                    return getBean(requiredType);
                } catch (NoSuchBeanDefinitionException ex) {
                    return null;
                }
            }
        };
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support resolution by ResolvableType");
    }

    @Override
    public boolean containsBean(String name) {
        if (this.singletonObjects.containsKey(name) || this.resourceTypes.containsKey(name)) {
            return true;
        }
        try {
            doGetType(name);
            return true;
        } catch (NamingException ex) {
            return false;
        }
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return this.shareableResources.contains(name);
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return !this.shareableResources.contains(name);
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        Class<?> type = getType(name);
        return (type != null && typeToMatch.isAssignableFrom(type));
    }

    @Override
    public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        Class<?> type = getType(name);
        return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
    }

    @Override
    @Nullable
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        try {
            return doGetType(name);
        } catch (NameNotFoundException ex) {
            throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
        } catch (NamingException ex) {
            return null;
        }
    }

    @Override
    public String[] getAliases(String name) {
        return new String[0];
    }

    @SuppressWarnings("unchecked")
    private <T> T doGetSingleton(String name, @Nullable Class<T> requiredType) throws NamingException {
        synchronized (this.singletonObjects) {
            Object singleton = this.singletonObjects.get(name);
            if (singleton != null) {
                if (requiredType != null && !requiredType.isInstance(singleton)) {
                    throw new TypeMismatchNamingException(convertJndiName(name), requiredType, singleton.getClass());
                }
                return (T) singleton;
            }
            T jndiObject = lookup(name, requiredType);
            this.singletonObjects.put(name, jndiObject);
            return jndiObject;
        }
    }

    private Class<?> doGetType(String name) throws NamingException {
        if (isSingleton(name)) {
            return doGetSingleton(name, null).getClass();
        } else {
            synchronized (this.resourceTypes) {
                Class<?> type = this.resourceTypes.get(name);
                if (type == null) {
                    type = lookup(name, null).getClass();
                    this.resourceTypes.put(name, type);
                }
                return type;
            }
        }
    }

}
