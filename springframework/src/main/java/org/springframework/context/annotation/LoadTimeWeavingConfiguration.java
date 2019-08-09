package org.springframework.context.annotation;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@Configuration
public class LoadTimeWeavingConfiguration implements ImportAware, BeanClassLoaderAware {

    @Nullable
    private AnnotationAttributes enableLTW;

    @Nullable
    private LoadTimeWeavingConfigurer ltwConfigurer;

    @Nullable
    private ClassLoader beanClassLoader;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableLTW = AnnotationConfigUtils.attributesFor(importMetadata, EnableLoadTimeWeaving.class);
        if (this.enableLTW == null) {
            throw new IllegalArgumentException("@EnableLoadTimeWeaving is not present on importing class " + importMetadata.getClassName());
        }
    }

    /**
     * 注入
     */
    @Autowired(required = false)
    public void setLoadTimeWeavingConfigurer(LoadTimeWeavingConfigurer ltwConfigurer) {
        this.ltwConfigurer = ltwConfigurer;
    }

    @Override
    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
    }

    /**
     * loadTimeWeaver
     */
    @Bean(name = ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LoadTimeWeaver loadTimeWeaver() {
        Assert.state(this.beanClassLoader != null, "No ClassLoader set");
        LoadTimeWeaver loadTimeWeaver = null;
        if (this.ltwConfigurer != null) {
            // 用户提供了自定义的LoadTimeWeaver实例
            loadTimeWeaver = this.ltwConfigurer.getLoadTimeWeaver();
        }
        if (loadTimeWeaver == null) {
            // 没有就默认DefaultContextLoadTimeWeaver
            loadTimeWeaver = new DefaultContextLoadTimeWeaver(this.beanClassLoader);
        }
        if (this.enableLTW != null) {
            AspectJWeaving aspectJWeaving = this.enableLTW.getEnum("aspectjWeaving");
            switch (aspectJWeaving) {
                case DISABLED:
                    // AJ weaving is disabled -> do nothing
                    break;
                case AUTODETECT:
                    if (this.beanClassLoader.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) == null) {
                        // No aop.xml present on the classpath -> treat as 'disabled'
                        break;
                    }
                    // aop.xml is present on the classpath -> enable
                    AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
                    break;
                case ENABLED:
                    AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
                    break;
            }
        }
        return loadTimeWeaver;
    }

}
