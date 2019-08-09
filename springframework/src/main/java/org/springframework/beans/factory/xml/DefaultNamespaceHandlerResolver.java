package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {

    public static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private final ClassLoader classLoader;

    private final String handlerMappingsLocation;

    @Nullable
    private volatile Map<String, Object> handlerMappings;

    public DefaultNamespaceHandlerResolver() {
        this(null, DEFAULT_HANDLER_MAPPINGS_LOCATION);
    }

    public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader) {
        this(classLoader, DEFAULT_HANDLER_MAPPINGS_LOCATION);
    }

    public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader, String handlerMappingsLocation) {
        Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
        this.handlerMappingsLocation = handlerMappingsLocation;
    }

    @Override
    @Nullable
    public NamespaceHandler resolve(String namespaceUri) {
        Map<String, Object> handlerMappings = getHandlerMappings();
        Object handlerOrClassName = handlerMappings.get(namespaceUri);
        if (handlerOrClassName == null) {
            return null;
        } else if (handlerOrClassName instanceof NamespaceHandler) {
            return (NamespaceHandler) handlerOrClassName;
        } else {
            String className = (String) handlerOrClassName;
            try {
                Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
                if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
                    throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri + "] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
                }
                NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
                namespaceHandler.init();
                handlerMappings.put(namespaceUri, namespaceHandler);
                return namespaceHandler;
            } catch (ClassNotFoundException ex) {
                throw new FatalBeanException("Could not find NamespaceHandler class [" + className + "] for namespace [" + namespaceUri + "]", ex);
            } catch (LinkageError err) {
                throw new FatalBeanException("Unresolvable class definition for NamespaceHandler class [" + className + "] for namespace [" + namespaceUri + "]", err);
            }
        }
    }

    private Map<String, Object> getHandlerMappings() {
        Map<String, Object> handlerMappings = this.handlerMappings;
        if (handlerMappings == null) {
            synchronized (this) {
                handlerMappings = this.handlerMappings;
                if (handlerMappings == null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Loading NamespaceHandler mappings from [" + this.handlerMappingsLocation + "]");
                    }
                    try {
                        Properties mappings = PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
                        if (logger.isTraceEnabled()) {
                            logger.trace("Loaded NamespaceHandler mappings: " + mappings);
                        }
                        handlerMappings = new ConcurrentHashMap<>(mappings.size());
                        CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
                        this.handlerMappings = handlerMappings;
                    } catch (IOException ex) {
                        throw new IllegalStateException("Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
                    }
                }
            }
        }
        return handlerMappings;
    }

    @Override
    public String toString() {
        return "NamespaceHandlerResolver using mappings " + getHandlerMappings();
    }

}
