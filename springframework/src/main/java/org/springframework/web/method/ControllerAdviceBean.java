package org.springframework.web.method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ControllerAdviceBean implements Ordered {

    private final Object bean;

    @Nullable
    private final BeanFactory beanFactory;

    private final int order;

    private final HandlerTypePredicate beanTypePredicate;

    public ControllerAdviceBean(Object bean) {
        this(bean, null);
    }

    public ControllerAdviceBean(String beanName, @Nullable BeanFactory beanFactory) {
        this((Object) beanName, beanFactory);
    }

    private ControllerAdviceBean(Object bean, @Nullable BeanFactory beanFactory) {
        this.bean = bean;
        this.beanFactory = beanFactory;
        Class<?> beanType;
        if (bean instanceof String) {
            String beanName = (String) bean;
            Assert.hasText(beanName, "Bean name must not be null");
            Assert.notNull(beanFactory, "BeanFactory must not be null");
            if (!beanFactory.containsBean(beanName)) {
                throw new IllegalArgumentException("BeanFactory [" + beanFactory + "] does not contain specified controller advice bean '" + beanName + "'");
            }
            beanType = this.beanFactory.getType(beanName);
            this.order = initOrderFromBeanType(beanType);
        } else {
            Assert.notNull(bean, "Bean must not be null");
            beanType = bean.getClass();
            this.order = initOrderFromBean(bean);
        }
        ControllerAdvice annotation = (beanType != null ? AnnotatedElementUtils.findMergedAnnotation(beanType, ControllerAdvice.class) : null);
        if (annotation != null) {
            this.beanTypePredicate = HandlerTypePredicate.builder().basePackage(annotation.basePackages()).basePackageClass(annotation.basePackageClasses()).assignableType(annotation.assignableTypes()).annotation(annotation.annotations()).build();
        } else {
            this.beanTypePredicate = HandlerTypePredicate.forAnyHandlerType();
        }
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Nullable
    public Class<?> getBeanType() {
        Class<?> beanType = (this.bean instanceof String ? obtainBeanFactory().getType((String) this.bean) : this.bean.getClass());
        return (beanType != null ? ClassUtils.getUserClass(beanType) : null);
    }

    public Object resolveBean() {
        return (this.bean instanceof String ? obtainBeanFactory().getBean((String) this.bean) : this.bean);
    }

    private BeanFactory obtainBeanFactory() {
        Assert.state(this.beanFactory != null, "No BeanFactory set");
        return this.beanFactory;
    }

    public boolean isApplicableToBeanType(@Nullable Class<?> beanType) {
        return this.beanTypePredicate.test(beanType);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ControllerAdviceBean)) {
            return false;
        }
        ControllerAdviceBean otherAdvice = (ControllerAdviceBean) other;
        return (this.bean.equals(otherAdvice.bean) && this.beanFactory == otherAdvice.beanFactory);
    }

    @Override
    public int hashCode() {
        return this.bean.hashCode();
    }

    @Override
    public String toString() {
        return this.bean.toString();
    }

    public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext context) {
        return Arrays.stream(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, Object.class)).filter(name -> context.findAnnotationOnBean(name, ControllerAdvice.class) != null).map(name -> new ControllerAdviceBean(name, context)).collect(Collectors.toList());
    }

    private static int initOrderFromBean(Object bean) {
        return (bean instanceof Ordered ? ((Ordered) bean).getOrder() : initOrderFromBeanType(bean.getClass()));
    }

    private static int initOrderFromBeanType(@Nullable Class<?> beanType) {
        Integer order = null;
        if (beanType != null) {
            order = OrderUtils.getOrder(beanType);
        }
        return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
    }

}
