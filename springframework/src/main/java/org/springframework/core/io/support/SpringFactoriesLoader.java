package org.springframework.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 加载META-INF/spring.factories
 */
public final class SpringFactoriesLoader {

    public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

    private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

    private static final Map<ClassLoader, MultiValueMap<String, String>> cache = new ConcurrentReferenceHashMap<>();

    private SpringFactoriesLoader() {
    }

    public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
        Assert.notNull(factoryType, "'factoryType' must not be null");
        ClassLoader classLoaderToUse = classLoader;
        if (classLoaderToUse == null) {
            classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
        }
        // 从META-INF/spring.factories读取key为factoryType类名列表
        List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoaderToUse);
        if (logger.isTraceEnabled()) {
            logger.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
        }
        List<T> result = new ArrayList<>(factoryImplementationNames.size());
        // 遍历
        for (String factoryImplementationName : factoryImplementationNames) {
            // 反射实例化
            result.add(instantiateFactory(factoryImplementationName, factoryType, classLoaderToUse));
        }
        AnnotationAwareOrderComparator.sort(result);
        return result;
    }

    public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
        // org.springframework.boot.autoconfigure.EnableAutoConfiguration
        String factoryTypeName = factoryType.getName();
        return loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
    }

    private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
        MultiValueMap<String, String> result = cache.get(classLoader);
        if (result != null) {
            return result;
        }
        try {
            Enumeration<URL> urls = (classLoader != null ? classLoader.getResources(FACTORIES_RESOURCE_LOCATION) : ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
            result = new LinkedMultiValueMap<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                UrlResource resource = new UrlResource(url);
                Properties properties = PropertiesLoaderUtils.loadProperties(resource);
                for (Map.Entry<?, ?> entry : properties.entrySet()) {
                    String factoryTypeName = ((String) entry.getKey()).trim();
                    for (String factoryImplementationName : StringUtils.commaDelimitedListToStringArray((String) entry.getValue())) {
                        result.add(factoryTypeName, factoryImplementationName.trim());
                    }
                }
            }
            cache.put(classLoader, result);
            return result;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load factories from location [" + FACTORIES_RESOURCE_LOCATION + "]", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiateFactory(String factoryImplementationName, Class<T> factoryType, ClassLoader classLoader) {
        try {
            Class<?> factoryImplementationClass = ClassUtils.forName(factoryImplementationName, classLoader);
            if (!factoryType.isAssignableFrom(factoryImplementationClass)) {
                throw new IllegalArgumentException("Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
            }
            return (T) ReflectionUtils.accessibleConstructor(factoryImplementationClass).newInstance();
        } catch (Throwable ex) {
            throw new IllegalArgumentException("Unable to instantiate factory class [" + factoryImplementationName + "] for factory type [" + factoryType.getName() + "]", ex);
        }
    }

}
