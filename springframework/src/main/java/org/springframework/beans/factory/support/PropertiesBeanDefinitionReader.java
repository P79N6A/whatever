package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public static final String TRUE_VALUE = "true";

    public static final String SEPARATOR = ".";

    public static final String CLASS_KEY = "(class)";

    public static final String PARENT_KEY = "(parent)";

    public static final String SCOPE_KEY = "(scope)";

    public static final String SINGLETON_KEY = "(singleton)";

    public static final String ABSTRACT_KEY = "(abstract)";

    public static final String LAZY_INIT_KEY = "(lazy-init)";

    public static final String REF_SUFFIX = "(ref)";

    public static final String REF_PREFIX = "*";

    public static final String CONSTRUCTOR_ARG_PREFIX = "$";

    @Nullable
    private String defaultParentBean;

    private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

    public PropertiesBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    public void setDefaultParentBean(@Nullable String defaultParentBean) {
        this.defaultParentBean = defaultParentBean;
    }

    @Nullable
    public String getDefaultParentBean() {
        return this.defaultParentBean;
    }

    public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
        this.propertiesPersister = (propertiesPersister != null ? propertiesPersister : new DefaultPropertiesPersister());
    }

    public PropertiesPersister getPropertiesPersister() {
        return this.propertiesPersister;
    }

    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(new EncodedResource(resource), null);
    }

    public int loadBeanDefinitions(Resource resource, @Nullable String prefix) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(new EncodedResource(resource), prefix);
    }

    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(encodedResource, null);
    }

    public int loadBeanDefinitions(EncodedResource encodedResource, @Nullable String prefix) throws BeanDefinitionStoreException {
        if (logger.isTraceEnabled()) {
            logger.trace("Loading properties bean definitions from " + encodedResource);
        }
        Properties props = new Properties();
        try {
            try (InputStream is = encodedResource.getResource().getInputStream()) {
                if (encodedResource.getEncoding() != null) {
                    getPropertiesPersister().load(props, new InputStreamReader(is, encodedResource.getEncoding()));
                } else {
                    getPropertiesPersister().load(props, is);
                }
            }
            int count = registerBeanDefinitions(props, prefix, encodedResource.getResource().getDescription());
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded " + count + " bean definitions from " + encodedResource);
            }
            return count;
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("Could not parse properties from " + encodedResource.getResource(), ex);
        }
    }

    public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
        return registerBeanDefinitions(rb, null);
    }

    public int registerBeanDefinitions(ResourceBundle rb, @Nullable String prefix) throws BeanDefinitionStoreException {
        // Simply create a map and call overloaded method.
        Map<String, Object> map = new HashMap<>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            map.put(key, rb.getObject(key));
        }
        return registerBeanDefinitions(map, prefix);
    }

    public int registerBeanDefinitions(Map<?, ?> map) throws BeansException {
        return registerBeanDefinitions(map, null);
    }

    public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix) throws BeansException {
        return registerBeanDefinitions(map, prefix, "Map " + map);
    }

    public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix, String resourceDescription) throws BeansException {
        if (prefix == null) {
            prefix = "";
        }
        int beanCount = 0;
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Illegal key [" + key + "]: only Strings allowed");
            }
            String keyString = (String) key;
            if (keyString.startsWith(prefix)) {
                // Key is of form: prefix<name>.property
                String nameAndProperty = keyString.substring(prefix.length());
                // Find dot before property name, ignoring dots in property keys.
                int sepIdx = -1;
                int propKeyIdx = nameAndProperty.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX);
                if (propKeyIdx != -1) {
                    sepIdx = nameAndProperty.lastIndexOf(SEPARATOR, propKeyIdx);
                } else {
                    sepIdx = nameAndProperty.lastIndexOf(SEPARATOR);
                }
                if (sepIdx != -1) {
                    String beanName = nameAndProperty.substring(0, sepIdx);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found bean name '" + beanName + "'");
                    }
                    if (!getRegistry().containsBeanDefinition(beanName)) {
                        // If we haven't already registered it...
                        registerBeanDefinition(beanName, map, prefix + beanName, resourceDescription);
                        ++beanCount;
                    }
                } else {
                    // Ignore it: It wasn't a valid bean name and property,
                    // although it did start with the required prefix.
                    if (logger.isDebugEnabled()) {
                        logger.debug("Invalid bean name and property [" + nameAndProperty + "]");
                    }
                }
            }
        }
        return beanCount;
    }

    protected void registerBeanDefinition(String beanName, Map<?, ?> map, String prefix, String resourceDescription) throws BeansException {
        String className = null;
        String parent = null;
        String scope = GenericBeanDefinition.SCOPE_SINGLETON;
        boolean isAbstract = false;
        boolean lazyInit = false;
        ConstructorArgumentValues cas = new ConstructorArgumentValues();
        MutablePropertyValues pvs = new MutablePropertyValues();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = StringUtils.trimWhitespace((String) entry.getKey());
            if (key.startsWith(prefix + SEPARATOR)) {
                String property = key.substring(prefix.length() + SEPARATOR.length());
                if (CLASS_KEY.equals(property)) {
                    className = StringUtils.trimWhitespace((String) entry.getValue());
                } else if (PARENT_KEY.equals(property)) {
                    parent = StringUtils.trimWhitespace((String) entry.getValue());
                } else if (ABSTRACT_KEY.equals(property)) {
                    String val = StringUtils.trimWhitespace((String) entry.getValue());
                    isAbstract = TRUE_VALUE.equals(val);
                } else if (SCOPE_KEY.equals(property)) {
                    // Spring 2.0 style
                    scope = StringUtils.trimWhitespace((String) entry.getValue());
                } else if (SINGLETON_KEY.equals(property)) {
                    // Spring 1.2 style
                    String val = StringUtils.trimWhitespace((String) entry.getValue());
                    scope = ("".equals(val) || TRUE_VALUE.equals(val) ? GenericBeanDefinition.SCOPE_SINGLETON : GenericBeanDefinition.SCOPE_PROTOTYPE);
                } else if (LAZY_INIT_KEY.equals(property)) {
                    String val = StringUtils.trimWhitespace((String) entry.getValue());
                    lazyInit = TRUE_VALUE.equals(val);
                } else if (property.startsWith(CONSTRUCTOR_ARG_PREFIX)) {
                    if (property.endsWith(REF_SUFFIX)) {
                        int index = Integer.parseInt(property.substring(1, property.length() - REF_SUFFIX.length()));
                        cas.addIndexedArgumentValue(index, new RuntimeBeanReference(entry.getValue().toString()));
                    } else {
                        int index = Integer.parseInt(property.substring(1));
                        cas.addIndexedArgumentValue(index, readValue(entry));
                    }
                } else if (property.endsWith(REF_SUFFIX)) {
                    // This isn't a real property, but a reference to another prototype
                    // Extract property name: property is of form dog(ref)
                    property = property.substring(0, property.length() - REF_SUFFIX.length());
                    String ref = StringUtils.trimWhitespace((String) entry.getValue());
                    // It doesn't matter if the referenced bean hasn't yet been registered:
                    // this will ensure that the reference is resolved at runtime.
                    Object val = new RuntimeBeanReference(ref);
                    pvs.add(property, val);
                } else {
                    // It's a normal bean property.
                    pvs.add(property, readValue(entry));
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Registering bean definition for bean name '" + beanName + "' with " + pvs);
        }
        // Just use default parent if we're not dealing with the parent itself,
        // and if there's no class name specified. The latter has to happen for
        // backwards compatibility reasons.
        if (parent == null && className == null && !beanName.equals(this.defaultParentBean)) {
            parent = this.defaultParentBean;
        }
        try {
            AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(parent, className, getBeanClassLoader());
            bd.setScope(scope);
            bd.setAbstract(isAbstract);
            bd.setLazyInit(lazyInit);
            bd.setConstructorArgumentValues(cas);
            bd.setPropertyValues(pvs);
            getRegistry().registerBeanDefinition(beanName, bd);
        } catch (ClassNotFoundException ex) {
            throw new CannotLoadBeanClassException(resourceDescription, beanName, className, ex);
        } catch (LinkageError err) {
            throw new CannotLoadBeanClassException(resourceDescription, beanName, className, err);
        }
    }

    private Object readValue(Map.Entry<?, ?> entry) {
        Object val = entry.getValue();
        if (val instanceof String) {
            String strVal = (String) val;
            // If it starts with a reference prefix...
            if (strVal.startsWith(REF_PREFIX)) {
                // Expand the reference.
                String targetName = strVal.substring(1);
                if (targetName.startsWith(REF_PREFIX)) {
                    // Escaped prefix -> use plain value.
                    val = targetName;
                } else {
                    val = new RuntimeBeanReference(targetName);
                }
            }
        }
        return val;
    }

}
