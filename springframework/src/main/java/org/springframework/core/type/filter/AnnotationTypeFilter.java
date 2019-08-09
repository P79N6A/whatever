package org.springframework.core.type.filter;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;

public class AnnotationTypeFilter extends AbstractTypeHierarchyTraversingFilter {

    private final Class<? extends Annotation> annotationType;

    private final boolean considerMetaAnnotations;

    public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
        this(annotationType, true, false);
    }

    public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations) {
        this(annotationType, considerMetaAnnotations, false);
    }

    public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean considerInterfaces) {
        super(annotationType.isAnnotationPresent(Inherited.class), considerInterfaces);
        this.annotationType = annotationType;
        this.considerMetaAnnotations = considerMetaAnnotations;
    }

    public final Class<? extends Annotation> getAnnotationType() {
        return this.annotationType;
    }

    @Override
    protected boolean matchSelf(MetadataReader metadataReader) {
        AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
        return metadata.hasAnnotation(this.annotationType.getName()) || (this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
    }

    @Override
    @Nullable
    protected Boolean matchSuperClass(String superClassName) {
        return hasAnnotation(superClassName);
    }

    @Override
    @Nullable
    protected Boolean matchInterface(String interfaceName) {
        return hasAnnotation(interfaceName);
    }

    @Nullable
    protected Boolean hasAnnotation(String typeName) {
        if (Object.class.getName().equals(typeName)) {
            return false;
        } else if (typeName.startsWith("java")) {
            if (!this.annotationType.getName().startsWith("java")) {
                // Standard Java types do not have non-standard annotations on them ->
                // skip any load attempt, in particular for Java language interfaces.
                return false;
            }
            try {
                Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
                return ((this.considerMetaAnnotations ? AnnotationUtils.getAnnotation(clazz, this.annotationType) : clazz.getAnnotation(this.annotationType)) != null);
            } catch (Throwable ex) {
                // Class not regularly loadable - can't determine a match that way.
            }
        }
        return null;
    }

}
