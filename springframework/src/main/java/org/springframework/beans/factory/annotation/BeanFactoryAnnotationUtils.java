package org.springframework.beans.factory.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public abstract class BeanFactoryAnnotationUtils {

    public static <T> Map<String, T> qualifiedBeansOfType(ListableBeanFactory beanFactory, Class<T> beanType, String qualifier) throws BeansException {
        String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType);
        Map<String, T> result = new LinkedHashMap<>(4);
        for (String beanName : candidateBeans) {
            if (isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
                result.put(beanName, beanFactory.getBean(beanName, beanType));
            }
        }
        return result;
    }

    public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier) throws BeansException {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        if (beanFactory instanceof ListableBeanFactory) {
            // Full qualifier matching supported.
            return qualifiedBeanOfType((ListableBeanFactory) beanFactory, beanType, qualifier);
        } else if (beanFactory.containsBean(qualifier)) {
            // Fallback: target bean at least found by bean name.
            return beanFactory.getBean(qualifier, beanType);
        } else {
            throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() + " bean found for bean name '" + qualifier + "'! (Note: Qualifier matching not supported because given " + "BeanFactory does not implement ConfigurableListableBeanFactory.)");
        }
    }

    private static <T> T qualifiedBeanOfType(ListableBeanFactory bf, Class<T> beanType, String qualifier) {
        String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(bf, beanType);
        String matchingBean = null;
        for (String beanName : candidateBeans) {
            if (isQualifierMatch(qualifier::equals, beanName, bf)) {
                if (matchingBean != null) {
                    throw new NoUniqueBeanDefinitionException(beanType, matchingBean, beanName);
                }
                matchingBean = beanName;
            }
        }
        if (matchingBean != null) {
            return bf.getBean(matchingBean, beanType);
        } else if (bf.containsBean(qualifier)) {
            // Fallback: target bean at least found by bean name - probably a manually registered singleton.
            return bf.getBean(qualifier, beanType);
        } else {
            throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() + " bean found for qualifier '" + qualifier + "' - neither qualifier match nor bean name match!");
        }
    }

    public static boolean isQualifierMatch(Predicate<String> qualifier, String beanName, @Nullable BeanFactory beanFactory) {
        // Try quick bean name or alias match first...
        if (qualifier.test(beanName)) {
            return true;
        }
        if (beanFactory != null) {
            for (String alias : beanFactory.getAliases(beanName)) {
                if (qualifier.test(alias)) {
                    return true;
                }
            }
            try {
                Class<?> beanType = beanFactory.getType(beanName);
                if (beanFactory instanceof ConfigurableBeanFactory) {
                    BeanDefinition bd = ((ConfigurableBeanFactory) beanFactory).getMergedBeanDefinition(beanName);
                    // Explicit qualifier metadata on bean definition? (typically in XML definition)
                    if (bd instanceof AbstractBeanDefinition) {
                        AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
                        AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
                        if (candidate != null) {
                            Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
                            if (value != null && qualifier.test(value.toString())) {
                                return true;
                            }
                        }
                    }
                    // Corresponding qualifier on factory method? (typically in configuration class)
                    if (bd instanceof RootBeanDefinition) {
                        Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
                        if (factoryMethod != null) {
                            Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
                            if (targetAnnotation != null) {
                                return qualifier.test(targetAnnotation.value());
                            }
                        }
                    }
                }
                // Corresponding qualifier on bean implementation class? (for custom user types)
                if (beanType != null) {
                    Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
                    if (targetAnnotation != null) {
                        return qualifier.test(targetAnnotation.value());
                    }
                }
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore - can't compare qualifiers for a manually registered singleton object
            }
        }
        return false;
    }

}
