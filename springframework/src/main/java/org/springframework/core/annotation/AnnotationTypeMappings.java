package org.springframework.core.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.annotation.Annotation;
import java.util.*;

final class AnnotationTypeMappings {

    private static final IntrospectionFailureLogger failureLogger = IntrospectionFailureLogger.DEBUG;

    private static final Map<AnnotationFilter, Cache> cache = new ConcurrentReferenceHashMap<>();

    private final AnnotationFilter filter;

    private final List<AnnotationTypeMapping> mappings;

    private AnnotationTypeMappings(AnnotationFilter filter, Class<? extends Annotation> annotationType) {
        this.filter = filter;
        this.mappings = new ArrayList<>();
        addAllMappings(annotationType);
        this.mappings.forEach(AnnotationTypeMapping::afterAllMappingsSet);
    }

    private void addAllMappings(Class<? extends Annotation> annotationType) {
        Deque<AnnotationTypeMapping> queue = new ArrayDeque<>();
        addIfPossible(queue, null, annotationType, null);
        while (!queue.isEmpty()) {
            AnnotationTypeMapping mapping = queue.removeFirst();
            this.mappings.add(mapping);
            addMetaAnnotationsToQueue(queue, mapping);
        }
    }

    private void addMetaAnnotationsToQueue(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping parent) {
        Annotation[] metaAnnotations = AnnotationsScanner.getDeclaredAnnotations(parent.getAnnotationType(), false);
        for (Annotation metaAnnotation : metaAnnotations) {
            if (!isMappable(parent, metaAnnotation)) {
                continue;
            }
            Annotation[] repeatedAnnotations = RepeatableContainers.standardRepeatables().findRepeatedAnnotations(metaAnnotation);
            if (repeatedAnnotations != null) {
                for (Annotation repeatedAnnotation : repeatedAnnotations) {
                    if (!isMappable(parent, metaAnnotation)) {
                        continue;
                    }
                    addIfPossible(queue, parent, repeatedAnnotation);
                }
            } else {
                addIfPossible(queue, parent, metaAnnotation);
            }
        }
    }

    private void addIfPossible(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping parent, Annotation ann) {
        addIfPossible(queue, parent, ann.annotationType(), ann);
    }

    private void addIfPossible(Deque<AnnotationTypeMapping> queue, @Nullable AnnotationTypeMapping parent, Class<? extends Annotation> annotationType, @Nullable Annotation ann) {
        try {
            queue.addLast(new AnnotationTypeMapping(parent, annotationType, ann));
        } catch (Exception ex) {
            if (ex instanceof AnnotationConfigurationException) {
                throw (AnnotationConfigurationException) ex;
            }
            if (failureLogger.isEnabled()) {
                failureLogger.log("Failed to introspect meta-annotation " + annotationType.getName(), (parent != null ? parent.getAnnotationType() : null), ex);
            }
        }
    }

    private boolean isMappable(AnnotationTypeMapping parent, @Nullable Annotation metaAnnotation) {
        return (metaAnnotation != null && !this.filter.matches(metaAnnotation) && !AnnotationFilter.PLAIN.matches(parent.getAnnotationType()) && !isAlreadyMapped(parent, metaAnnotation));
    }

    private boolean isAlreadyMapped(AnnotationTypeMapping parent, Annotation metaAnnotation) {
        Class<? extends Annotation> annotationType = metaAnnotation.annotationType();
        AnnotationTypeMapping mapping = parent;
        while (mapping != null) {
            if (mapping.getAnnotationType() == annotationType) {
                return true;
            }
            mapping = mapping.getParent();
        }
        return false;
    }

    int size() {
        return this.mappings.size();
    }

    AnnotationTypeMapping get(int index) {
        return this.mappings.get(index);
    }

    static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType) {
        return forAnnotationType(annotationType, AnnotationFilter.PLAIN);
    }

    static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType, AnnotationFilter annotationFilter) {
        return cache.computeIfAbsent(annotationFilter, Cache::new).get(annotationType);
    }

    static void clearCache() {
        cache.clear();
    }

    private static class Cache {

        private final AnnotationFilter filter;

        private final Map<Class<? extends Annotation>, AnnotationTypeMappings> mappings;

        Cache(AnnotationFilter filter) {
            this.filter = filter;
            this.mappings = new ConcurrentReferenceHashMap<>();
        }

        AnnotationTypeMappings get(Class<? extends Annotation> annotationType) {
            return this.mappings.computeIfAbsent(annotationType, this::createMappings);
        }

        AnnotationTypeMappings createMappings(Class<? extends Annotation> annotationType) {
            return new AnnotationTypeMappings(this.filter, annotationType);
        }

    }

}
