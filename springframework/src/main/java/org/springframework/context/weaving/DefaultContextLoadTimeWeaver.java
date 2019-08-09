package org.springframework.context.weaving;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver;
import org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.instrument.ClassFileTransformer;

/**
 * DefaultContextLoadTimeWeaver包装了实际的LoadTimeWeaver
 */
public class DefaultContextLoadTimeWeaver implements LoadTimeWeaver, BeanClassLoaderAware, DisposableBean {
    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private LoadTimeWeaver loadTimeWeaver;

    public DefaultContextLoadTimeWeaver() {
    }

    public DefaultContextLoadTimeWeaver(ClassLoader beanClassLoader) {
        setBeanClassLoader(beanClassLoader);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        Assert.state(this.loadTimeWeaver != null, "Not initialized");
        this.loadTimeWeaver.addTransformer(transformer);
    }

    /**
     * 根据环境创建相应的LoadTimeWeaver，不同容器有自己的类加载器，注册Transformer方式不同
     */
    @Nullable
    protected LoadTimeWeaver createServerSpecificLoadTimeWeaver(ClassLoader classLoader) {
        String name = classLoader.getClass().getName();
        try {
            // Tomcat
            if (name.startsWith("org.apache.catalina")) {
                return new TomcatLoadTimeWeaver(classLoader);
            }
            // ...
        } catch (Exception ex) {
            if (logger.isInfoEnabled()) {
                logger.info("Could not obtain server-specific LoadTimeWeaver: " + ex.getMessage());
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        if (this.loadTimeWeaver instanceof InstrumentationLoadTimeWeaver) {
            if (logger.isDebugEnabled()) {
                logger.debug("Removing all registered transformers for class loader: " + this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
            }
            ((InstrumentationLoadTimeWeaver) this.loadTimeWeaver).removeTransformers();
        }
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        LoadTimeWeaver serverSpecificLoadTimeWeaver = createServerSpecificLoadTimeWeaver(classLoader);
        // Web服务器
        if (serverSpecificLoadTimeWeaver != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Determined server-specific load-time weaver: " + serverSpecificLoadTimeWeaver.getClass().getName());
            }
            this.loadTimeWeaver = serverSpecificLoadTimeWeaver;
        }
        // spring-instrument.jar -javaagent
        else if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
            logger.debug("Found Spring's JVM agent for instrumentation");
            this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(classLoader);
        }
        // 默认sun.misc.Launcher.AppClassLoader
        else {
            try {
                this.loadTimeWeaver = new ReflectiveLoadTimeWeaver(classLoader);
                if (logger.isDebugEnabled()) {
                    logger.debug("Using reflective load-time weaver for class loader: " + this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
                }
            } catch (IllegalStateException ex) {
                throw new IllegalStateException(ex.getMessage() + " Specify a custom LoadTimeWeaver or start your " + "Java virtual machine with Spring's agent: -javaagent:spring-instrument-{version}.jar");
            }
        }
    }

    @Override
    public ClassLoader getInstrumentableClassLoader() {
        Assert.state(this.loadTimeWeaver != null, "Not initialized");
        return this.loadTimeWeaver.getInstrumentableClassLoader();
    }

    @Override
    public ClassLoader getThrowawayClassLoader() {
        Assert.state(this.loadTimeWeaver != null, "Not initialized");
        return this.loadTimeWeaver.getThrowawayClassLoader();
    }

}

