package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.beans.factory.wiring.BeanWiringInfoResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class AnnotationBeanWiringInfoResolver implements BeanWiringInfoResolver {

    @Override
    @Nullable
    public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
        Assert.notNull(beanInstance, "Bean instance must not be null");
        Configurable annotation = beanInstance.getClass().getAnnotation(Configurable.class);
        return (annotation != null ? buildWiringInfo(beanInstance, annotation) : null);
    }

    protected BeanWiringInfo buildWiringInfo(Object beanInstance, Configurable annotation) {
        if (!Autowire.NO.equals(annotation.autowire())) {
            // Autowiring by name or by type
            return new BeanWiringInfo(annotation.autowire().value(), annotation.dependencyCheck());
        } else if (!"".equals(annotation.value())) {
            // Explicitly specified bean name for bean definition to take property values from
            return new BeanWiringInfo(annotation.value(), false);
        } else {
            // Default bean name for bean definition to take property values from
            return new BeanWiringInfo(getDefaultBeanName(beanInstance), true);
        }
    }

    protected String getDefaultBeanName(Object beanInstance) {
        return ClassUtils.getUserClass(beanInstance).getName();
    }

}
