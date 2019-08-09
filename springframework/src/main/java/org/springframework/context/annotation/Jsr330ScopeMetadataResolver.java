package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Jsr330ScopeMetadataResolver implements ScopeMetadataResolver {

    private final Map<String, String> scopeMap = new HashMap<>();

    public Jsr330ScopeMetadataResolver() {
        registerScope("javax.inject.Singleton", BeanDefinition.SCOPE_SINGLETON);
    }

    public final void registerScope(Class<?> annotationType, String scopeName) {
        this.scopeMap.put(annotationType.getName(), scopeName);
    }

    public final void registerScope(String annotationType, String scopeName) {
        this.scopeMap.put(annotationType, scopeName);
    }

    @Nullable
    protected String resolveScopeName(String annotationType) {
        return this.scopeMap.get(annotationType);
    }

    @Override
    public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
        ScopeMetadata metadata = new ScopeMetadata();
        metadata.setScopeName(BeanDefinition.SCOPE_PROTOTYPE);
        if (definition instanceof AnnotatedBeanDefinition) {
            AnnotatedBeanDefinition annDef = (AnnotatedBeanDefinition) definition;
            Set<String> annTypes = annDef.getMetadata().getAnnotationTypes();
            String found = null;
            for (String annType : annTypes) {
                Set<String> metaAnns = annDef.getMetadata().getMetaAnnotationTypes(annType);
                if (metaAnns.contains("javax.inject.Scope")) {
                    if (found != null) {
                        throw new IllegalStateException("Found ambiguous scope annotations on bean class [" + definition.getBeanClassName() + "]: " + found + ", " + annType);
                    }
                    found = annType;
                    String scopeName = resolveScopeName(annType);
                    if (scopeName == null) {
                        throw new IllegalStateException("Unsupported scope annotation - not mapped onto Spring scope name: " + annType);
                    }
                    metadata.setScopeName(scopeName);
                }
            }
        }
        return metadata;
    }

}
