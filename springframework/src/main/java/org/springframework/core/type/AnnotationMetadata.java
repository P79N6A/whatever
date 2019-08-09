package org.springframework.core.type;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

    default Set<String> getAnnotationTypes() {
        return getAnnotations().stream().filter(MergedAnnotation::isDirectlyPresent).map(annotation -> annotation.getType().getName()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default Set<String> getMetaAnnotationTypes(String annotationName) {
        MergedAnnotation<?> annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
        if (!annotation.isPresent()) {
            return Collections.emptySet();
        }
        return MergedAnnotations.from(annotation.getType(), SearchStrategy.INHERITED_ANNOTATIONS).stream().map(mergedAnnotation -> mergedAnnotation.getType().getName()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default boolean hasAnnotation(String annotationName) {
        return getAnnotations().isDirectlyPresent(annotationName);
    }

    default boolean hasMetaAnnotation(String metaAnnotationName) {
        return getAnnotations().get(metaAnnotationName, MergedAnnotation::isMetaPresent).isPresent();
    }

    default boolean hasAnnotatedMethods(String annotationName) {
        return !getAnnotatedMethods(annotationName).isEmpty();
    }

    Set<MethodMetadata> getAnnotatedMethods(String annotationName);

    static AnnotationMetadata introspect(Class<?> type) {
        return StandardAnnotationMetadata.from(type);
    }

}
