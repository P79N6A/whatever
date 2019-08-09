package org.springframework.beans.factory.annotation;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.GenericTypeAwareAutowireCandidateResolver;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class QualifierAnnotationAutowireCandidateResolver extends GenericTypeAwareAutowireCandidateResolver {

    private final Set<Class<? extends Annotation>> qualifierTypes = new LinkedHashSet<>(2);

    private Class<? extends Annotation> valueAnnotationType = Value.class;

    @SuppressWarnings("unchecked")
    public QualifierAnnotationAutowireCandidateResolver() {
        this.qualifierTypes.add(Qualifier.class);
        try {
            this.qualifierTypes.add((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Qualifier", QualifierAnnotationAutowireCandidateResolver.class.getClassLoader()));
        } catch (ClassNotFoundException ex) {
            // JSR-330 API not available - simply skip.
        }
    }

    public QualifierAnnotationAutowireCandidateResolver(Class<? extends Annotation> qualifierType) {
        Assert.notNull(qualifierType, "'qualifierType' must not be null");
        this.qualifierTypes.add(qualifierType);
    }

    public QualifierAnnotationAutowireCandidateResolver(Set<Class<? extends Annotation>> qualifierTypes) {
        Assert.notNull(qualifierTypes, "'qualifierTypes' must not be null");
        this.qualifierTypes.addAll(qualifierTypes);
    }

    public void addQualifierType(Class<? extends Annotation> qualifierType) {
        this.qualifierTypes.add(qualifierType);
    }

    public void setValueAnnotationType(Class<? extends Annotation> valueAnnotationType) {
        this.valueAnnotationType = valueAnnotationType;
    }

    @Override
    public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        boolean match = super.isAutowireCandidate(bdHolder, descriptor);
        if (match) {
            match = checkQualifiers(bdHolder, descriptor.getAnnotations());
            if (match) {
                MethodParameter methodParam = descriptor.getMethodParameter();
                if (methodParam != null) {
                    Method method = methodParam.getMethod();
                    if (method == null || void.class == method.getReturnType()) {
                        match = checkQualifiers(bdHolder, methodParam.getMethodAnnotations());
                    }
                }
            }
        }
        return match;
    }

    protected boolean checkQualifiers(BeanDefinitionHolder bdHolder, Annotation[] annotationsToSearch) {
        if (ObjectUtils.isEmpty(annotationsToSearch)) {
            return true;
        }
        SimpleTypeConverter typeConverter = new SimpleTypeConverter();
        for (Annotation annotation : annotationsToSearch) {
            Class<? extends Annotation> type = annotation.annotationType();
            boolean checkMeta = true;
            boolean fallbackToMeta = false;
            if (isQualifier(type)) {
                if (!checkQualifier(bdHolder, annotation, typeConverter)) {
                    fallbackToMeta = true;
                } else {
                    checkMeta = false;
                }
            }
            if (checkMeta) {
                boolean foundMeta = false;
                for (Annotation metaAnn : type.getAnnotations()) {
                    Class<? extends Annotation> metaType = metaAnn.annotationType();
                    if (isQualifier(metaType)) {
                        foundMeta = true;
                        // Only accept fallback match if @Qualifier annotation has a value...
                        // Otherwise it is just a marker for a custom qualifier annotation.
                        if ((fallbackToMeta && StringUtils.isEmpty(AnnotationUtils.getValue(metaAnn))) || !checkQualifier(bdHolder, metaAnn, typeConverter)) {
                            return false;
                        }
                    }
                }
                if (fallbackToMeta && !foundMeta) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isQualifier(Class<? extends Annotation> annotationType) {
        for (Class<? extends Annotation> qualifierType : this.qualifierTypes) {
            if (annotationType.equals(qualifierType) || annotationType.isAnnotationPresent(qualifierType)) {
                return true;
            }
        }
        return false;
    }

    protected boolean checkQualifier(BeanDefinitionHolder bdHolder, Annotation annotation, TypeConverter typeConverter) {
        Class<? extends Annotation> type = annotation.annotationType();
        RootBeanDefinition bd = (RootBeanDefinition) bdHolder.getBeanDefinition();
        AutowireCandidateQualifier qualifier = bd.getQualifier(type.getName());
        if (qualifier == null) {
            qualifier = bd.getQualifier(ClassUtils.getShortName(type));
        }
        if (qualifier == null) {
            // First, check annotation on qualified element, if any
            Annotation targetAnnotation = getQualifiedElementAnnotation(bd, type);
            // Then, check annotation on factory method, if applicable
            if (targetAnnotation == null) {
                targetAnnotation = getFactoryMethodAnnotation(bd, type);
            }
            if (targetAnnotation == null) {
                RootBeanDefinition dbd = getResolvedDecoratedDefinition(bd);
                if (dbd != null) {
                    targetAnnotation = getFactoryMethodAnnotation(dbd, type);
                }
            }
            if (targetAnnotation == null) {
                // Look for matching annotation on the target class
                if (getBeanFactory() != null) {
                    try {
                        Class<?> beanType = getBeanFactory().getType(bdHolder.getBeanName());
                        if (beanType != null) {
                            targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(beanType), type);
                        }
                    } catch (NoSuchBeanDefinitionException ex) {
                        // Not the usual case - simply forget about the type check...
                    }
                }
                if (targetAnnotation == null && bd.hasBeanClass()) {
                    targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(bd.getBeanClass()), type);
                }
            }
            if (targetAnnotation != null && targetAnnotation.equals(annotation)) {
                return true;
            }
        }
        Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
        if (attributes.isEmpty() && qualifier == null) {
            // If no attributes, the qualifier must be present
            return false;
        }
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            Object expectedValue = entry.getValue();
            Object actualValue = null;
            // Check qualifier first
            if (qualifier != null) {
                actualValue = qualifier.getAttribute(attributeName);
            }
            if (actualValue == null) {
                // Fall back on bean definition attribute
                actualValue = bd.getAttribute(attributeName);
            }
            if (actualValue == null && attributeName.equals(AutowireCandidateQualifier.VALUE_KEY) && expectedValue instanceof String && bdHolder.matchesName((String) expectedValue)) {
                // Fall back on bean name (or alias) match
                continue;
            }
            if (actualValue == null && qualifier != null) {
                // Fall back on default, but only if the qualifier is present
                actualValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
            }
            if (actualValue != null) {
                actualValue = typeConverter.convertIfNecessary(actualValue, expectedValue.getClass());
            }
            if (!expectedValue.equals(actualValue)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    protected Annotation getQualifiedElementAnnotation(RootBeanDefinition bd, Class<? extends Annotation> type) {
        AnnotatedElement qualifiedElement = bd.getQualifiedElement();
        return (qualifiedElement != null ? AnnotationUtils.getAnnotation(qualifiedElement, type) : null);
    }

    @Nullable
    protected Annotation getFactoryMethodAnnotation(RootBeanDefinition bd, Class<? extends Annotation> type) {
        Method resolvedFactoryMethod = bd.getResolvedFactoryMethod();
        return (resolvedFactoryMethod != null ? AnnotationUtils.getAnnotation(resolvedFactoryMethod, type) : null);
    }

    @Override
    public boolean isRequired(DependencyDescriptor descriptor) {
        if (!super.isRequired(descriptor)) {
            return false;
        }
        Autowired autowired = descriptor.getAnnotation(Autowired.class);
        return (autowired == null || autowired.required());
    }

    @Override
    public boolean hasQualifier(DependencyDescriptor descriptor) {
        for (Annotation ann : descriptor.getAnnotations()) {
            if (isQualifier(ann.annotationType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        Object value = findValue(descriptor.getAnnotations());
        if (value == null) {
            MethodParameter methodParam = descriptor.getMethodParameter();
            if (methodParam != null) {
                value = findValue(methodParam.getMethodAnnotations());
            }
        }
        return value;
    }

    @Nullable
    protected Object findValue(Annotation[] annotationsToSearch) {
        if (annotationsToSearch.length > 0) {   // qualifier annotations have to be local
            AnnotationAttributes attr = AnnotatedElementUtils.getMergedAnnotationAttributes(AnnotatedElementUtils.forAnnotations(annotationsToSearch), this.valueAnnotationType);
            if (attr != null) {
                return extractValue(attr);
            }
        }
        return null;
    }

    protected Object extractValue(AnnotationAttributes attr) {
        Object value = attr.get(AnnotationUtils.VALUE);
        if (value == null) {
            throw new IllegalStateException("Value annotation must have a value attribute");
        }
        return value;
    }

}
