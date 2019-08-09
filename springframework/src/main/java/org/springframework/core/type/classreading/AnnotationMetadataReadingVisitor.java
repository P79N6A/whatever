package org.springframework.core.type.classreading;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@Deprecated
public class AnnotationMetadataReadingVisitor extends ClassMetadataReadingVisitor implements AnnotationMetadata {

    @Nullable
    protected final ClassLoader classLoader;

    protected final Set<String> annotationSet = new LinkedHashSet<>(4);

    protected final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<>(4);

    protected final LinkedMultiValueMap<String, AnnotationAttributes> attributesMap = new LinkedMultiValueMap<>(4);

    protected final Set<MethodMetadata> methodMetadataSet = new LinkedHashSet<>(4);

    public AnnotationMetadataReadingVisitor(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public MergedAnnotations getAnnotations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // Skip bridge methods - we're only interested in original annotation-defining user methods.
        // On JDK 8, we'd otherwise run into double detection of the same annotated method...
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
        return new MethodMetadataReadingVisitor(name, access, getClassName(), Type.getReturnType(desc).getClassName(), this.classLoader, this.methodMetadataSet);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (!visible) {
            return null;
        }
        String className = Type.getType(desc).getClassName();
        if (AnnotationUtils.isInJavaLangAnnotationPackage(className)) {
            return null;
        }
        this.annotationSet.add(className);
        return new AnnotationAttributesReadingVisitor(className, this.attributesMap, this.metaAnnotationMap, this.classLoader);
    }

    @Override
    public Set<String> getAnnotationTypes() {
        return this.annotationSet;
    }

    @Override
    public Set<String> getMetaAnnotationTypes(String annotationName) {
        Set<String> metaAnnotationTypes = this.metaAnnotationMap.get(annotationName);
        return (metaAnnotationTypes != null ? metaAnnotationTypes : Collections.emptySet());
    }

    @Override
    public boolean hasMetaAnnotation(String metaAnnotationType) {
        if (AnnotationUtils.isInJavaLangAnnotationPackage(metaAnnotationType)) {
            return false;
        }
        Collection<Set<String>> allMetaTypes = this.metaAnnotationMap.values();
        for (Set<String> metaTypes : allMetaTypes) {
            if (metaTypes.contains(metaAnnotationType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAnnotated(String annotationName) {
        return (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName) && this.attributesMap.containsKey(annotationName));
    }

    @Override
    public boolean hasAnnotation(String annotationName) {
        return getAnnotationTypes().contains(annotationName);
    }

    @Override
    @Nullable
    public AnnotationAttributes getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        AnnotationAttributes raw = AnnotationReadingVisitorUtils.getMergedAnnotationAttributes(this.attributesMap, this.metaAnnotationMap, annotationName);
        if (raw == null) {
            return null;
        }
        return AnnotationReadingVisitorUtils.convertClassValues("class '" + getClassName() + "'", this.classLoader, raw, classValuesAsString);
    }

    @Override
    @Nullable
    public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        MultiValueMap<String, Object> allAttributes = new LinkedMultiValueMap<>();
        List<AnnotationAttributes> attributes = this.attributesMap.get(annotationName);
        if (attributes == null) {
            return null;
        }
        for (AnnotationAttributes raw : attributes) {
            for (Map.Entry<String, Object> entry : AnnotationReadingVisitorUtils.convertClassValues("class '" + getClassName() + "'", this.classLoader, raw, classValuesAsString).entrySet()) {
                allAttributes.add(entry.getKey(), entry.getValue());
            }
        }
        return allAttributes;
    }

    @Override
    public boolean hasAnnotatedMethods(String annotationName) {
        for (MethodMetadata methodMetadata : this.methodMetadataSet) {
            if (methodMetadata.isAnnotated(annotationName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
        Set<MethodMetadata> annotatedMethods = new LinkedHashSet<>(4);
        for (MethodMetadata methodMetadata : this.methodMetadataSet) {
            if (methodMetadata.isAnnotated(annotationName)) {
                annotatedMethods.add(methodMetadata);
            }
        }
        return annotatedMethods;
    }

}
