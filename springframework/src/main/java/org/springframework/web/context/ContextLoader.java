package org.springframework.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ContextLoader {

    public static final String CONTEXT_ID_PARAM = "contextId";

    public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

    public static final String CONTEXT_CLASS_PARAM = "contextClass";

    public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

    public static final String GLOBAL_INITIALIZER_CLASSES_PARAM = "globalInitializerClasses";

    private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

    private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";

    private static final Properties defaultStrategies;

    static {
        // Load default strategy implementations from properties file.
        // This is currently strictly internal and not meant to be customized
        // by application developers.
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
            defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
        }
    }

    private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread = new ConcurrentHashMap<>(1);

    @Nullable
    private static volatile WebApplicationContext currentContext;

    @Nullable
    private WebApplicationContext context;

    private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers = new ArrayList<>();

    public ContextLoader() {
    }

    public ContextLoader(WebApplicationContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
        if (initializers != null) {
            for (ApplicationContextInitializer<?> initializer : initializers) {
                this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
            }
        }
    }

    public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
        if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
            throw new IllegalStateException("Cannot initialize context because there is already a root application context present - " + "check whether you have multiple ContextLoader* definitions in your web.xml!");
        }
        servletContext.log("Initializing Spring root WebApplicationContext");
        Log logger = LogFactory.getLog(ContextLoader.class);
        if (logger.isInfoEnabled()) {
            logger.info("Root WebApplicationContext: initialization started");
        }
        long startTime = System.currentTimeMillis();
        try {
            // Store context in local instance variable, to guarantee that
            // it is available on ServletContext shutdown.
            if (this.context == null) {
                this.context = createWebApplicationContext(servletContext);
            }
            if (this.context instanceof ConfigurableWebApplicationContext) {
                ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
                if (!cwac.isActive()) {
                    // The context has not yet been refreshed -> provide services such as
                    // setting the parent context, setting the application context id, etc
                    if (cwac.getParent() == null) {
                        // The context instance was injected without an explicit parent ->
                        // determine parent for root web application context, if any.
                        ApplicationContext parent = loadParentContext(servletContext);
                        cwac.setParent(parent);
                    }
                    configureAndRefreshWebApplicationContext(cwac, servletContext);
                }
            }
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
            ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            if (ccl == ContextLoader.class.getClassLoader()) {
                currentContext = this.context;
            } else if (ccl != null) {
                currentContextPerThread.put(ccl, this.context);
            }
            if (logger.isInfoEnabled()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                logger.info("Root WebApplicationContext initialized in " + elapsedTime + " ms");
            }
            return this.context;
        } catch (RuntimeException | Error ex) {
            logger.error("Context initialization failed", ex);
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
            throw ex;
        }
    }

    protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
        Class<?> contextClass = determineContextClass(sc);
        if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
            throw new ApplicationContextException("Custom context class [" + contextClass.getName() + "] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
        }
        return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
    }

    protected Class<?> determineContextClass(ServletContext servletContext) {
        String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
        if (contextClassName != null) {
            try {
                return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
            } catch (ClassNotFoundException ex) {
                throw new ApplicationContextException("Failed to load custom context class [" + contextClassName + "]", ex);
            }
        } else {
            contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
            try {
                return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
            } catch (ClassNotFoundException ex) {
                throw new ApplicationContextException("Failed to load default context class [" + contextClassName + "]", ex);
            }
        }
    }

    protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
        if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
            // The application context id is still set to its original default value
            // -> assign a more useful id based on available information
            String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
            if (idParam != null) {
                wac.setId(idParam);
            } else {
                // Generate default id...
                wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX + ObjectUtils.getDisplayString(sc.getContextPath()));
            }
        }
        wac.setServletContext(sc);
        String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
        if (configLocationParam != null) {
            wac.setConfigLocation(configLocationParam);
        }
        // The wac environment's #initPropertySources will be called in any case when the context
        // is refreshed; do it eagerly here to ensure servlet property sources are in place for
        // use in any post-processing or initialization that occurs below prior to #refresh
        ConfigurableEnvironment env = wac.getEnvironment();
        if (env instanceof ConfigurableWebEnvironment) {
            ((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
        }
        customizeContext(sc, wac);
        wac.refresh();
    }

    protected void customizeContext(ServletContext sc, ConfigurableWebApplicationContext wac) {
        List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses = determineContextInitializerClasses(sc);
        for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
            Class<?> initializerContextClass = GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
            if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
                throw new ApplicationContextException(String.format("Could not apply context initializer [%s] since its generic parameter [%s] " + "is not assignable from the type of application context used by this " + "context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(), wac.getClass().getName()));
            }
            this.contextInitializers.add(BeanUtils.instantiateClass(initializerClass));
        }
        AnnotationAwareOrderComparator.sort(this.contextInitializers);
        for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
            initializer.initialize(wac);
        }
    }

    protected List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> determineContextInitializerClasses(ServletContext servletContext) {
        List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes = new ArrayList<>();
        String globalClassNames = servletContext.getInitParameter(GLOBAL_INITIALIZER_CLASSES_PARAM);
        if (globalClassNames != null) {
            for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
                classes.add(loadInitializerClass(className));
            }
        }
        String localClassNames = servletContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
        if (localClassNames != null) {
            for (String className : StringUtils.tokenizeToStringArray(localClassNames, INIT_PARAM_DELIMITERS)) {
                classes.add(loadInitializerClass(className));
            }
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    private Class<ApplicationContextInitializer<ConfigurableApplicationContext>> loadInitializerClass(String className) {
        try {
            Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
            if (!ApplicationContextInitializer.class.isAssignableFrom(clazz)) {
                throw new ApplicationContextException("Initializer class does not implement ApplicationContextInitializer interface: " + clazz);
            }
            return (Class<ApplicationContextInitializer<ConfigurableApplicationContext>>) clazz;
        } catch (ClassNotFoundException ex) {
            throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
        }
    }

    @Nullable
    protected ApplicationContext loadParentContext(ServletContext servletContext) {
        return null;
    }

    public void closeWebApplicationContext(ServletContext servletContext) {
        servletContext.log("Closing Spring root WebApplicationContext");
        try {
            if (this.context instanceof ConfigurableWebApplicationContext) {
                ((ConfigurableWebApplicationContext) this.context).close();
            }
        } finally {
            ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            if (ccl == ContextLoader.class.getClassLoader()) {
                currentContext = null;
            } else if (ccl != null) {
                currentContextPerThread.remove(ccl);
            }
            servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        }
    }

    @Nullable
    public static WebApplicationContext getCurrentWebApplicationContext() {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (ccl != null) {
            WebApplicationContext ccpt = currentContextPerThread.get(ccl);
            if (ccpt != null) {
                return ccpt;
            }
        }
        return currentContext;
    }

}
