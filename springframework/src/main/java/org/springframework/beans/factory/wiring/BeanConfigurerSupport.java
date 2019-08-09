package org.springframework.beans.factory.wiring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class BeanConfigurerSupport implements BeanFactoryAware, InitializingBean, DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private volatile BeanWiringInfoResolver beanWiringInfoResolver;

    @Nullable
    private volatile ConfigurableListableBeanFactory beanFactory;

    public void setBeanWiringInfoResolver(BeanWiringInfoResolver beanWiringInfoResolver) {
        Assert.notNull(beanWiringInfoResolver, "BeanWiringInfoResolver must not be null");
        this.beanWiringInfoResolver = beanWiringInfoResolver;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException("Bean configurer aspect needs to run in a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
        if (this.beanWiringInfoResolver == null) {
            this.beanWiringInfoResolver = createDefaultBeanWiringInfoResolver();
        }
    }

    @Nullable
    protected BeanWiringInfoResolver createDefaultBeanWiringInfoResolver() {
        return new ClassNameBeanWiringInfoResolver();
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.beanFactory, "BeanFactory must be set");
    }

    @Override
    public void destroy() {
        this.beanFactory = null;
        this.beanWiringInfoResolver = null;
    }

    public void configureBean(Object beanInstance) {
        if (this.beanFactory == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("BeanFactory has not been set on " + ClassUtils.getShortName(getClass()) + ": " + "Make sure this configurer runs in a Spring container. Unable to configure bean of type [" + ClassUtils.getDescriptiveType(beanInstance) + "]. Proceeding without injection.");
            }
            return;
        }
        BeanWiringInfoResolver bwiResolver = this.beanWiringInfoResolver;
        Assert.state(bwiResolver != null, "No BeanWiringInfoResolver available");
        BeanWiringInfo bwi = bwiResolver.resolveWiringInfo(beanInstance);
        if (bwi == null) {
            // Skip the bean if no wiring info given.
            return;
        }
        ConfigurableListableBeanFactory beanFactory = this.beanFactory;
        Assert.state(beanFactory != null, "No BeanFactory available");
        try {
            String beanName = bwi.getBeanName();
            if (bwi.indicatesAutowiring() || (bwi.isDefaultBeanName() && beanName != null && !beanFactory.containsBean(beanName))) {
                // Perform autowiring (also applying standard factory / post-processor callbacks).
                beanFactory.autowireBeanProperties(beanInstance, bwi.getAutowireMode(), bwi.getDependencyCheck());
                beanFactory.initializeBean(beanInstance, (beanName != null ? beanName : ""));
            } else {
                // Perform explicit wiring based on the specified bean definition.
                beanFactory.configureBean(beanInstance, (beanName != null ? beanName : ""));
            }
        } catch (BeanCreationException ex) {
            Throwable rootCause = ex.getMostSpecificCause();
            if (rootCause instanceof BeanCurrentlyInCreationException) {
                BeanCreationException bce = (BeanCreationException) rootCause;
                String bceBeanName = bce.getBeanName();
                if (bceBeanName != null && beanFactory.isCurrentlyInCreation(bceBeanName)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to create target bean '" + bce.getBeanName() + "' while configuring object of type [" + beanInstance.getClass().getName() + "] - probably due to a circular reference. This is a common startup situation " + "and usually not fatal. Proceeding without injection. Original exception: " + ex);
                    }
                    return;
                }
            }
            throw ex;
        }
    }

}
