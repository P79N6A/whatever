package org.springframework.core.type;

import org.springframework.core.annotation.*;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.Map;

public interface AnnotatedTypeMetadata {

    MergedAnnotations getAnnotations();

    default boolean isAnnotated(String annotationName) {
        return getAnnotations().isPresent(annotationName);
    }

    @Nullable
    default Map<String, Object> getAnnotationAttributes(String annotationName) {
        return getAnnotationAttributes(annotationName, false);
    }

    @Nullable
    default Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
        if (!annotation.isPresent()) {
            return null;
        }
        return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
    }

    @Nullable
    default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
        return getAllAnnotationAttributes(annotationName, false);
    }

    @Nullable
    default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        Adapt[] adaptations = Adapt.values(classValuesAsString, true);
        return getAnnotations().stream(annotationName).filter(MergedAnnotationPredicates.unique(MergedAnnotation::getTypeHierarchy)).map(MergedAnnotation::withNonMergedAttributes).collect(MergedAnnotationCollectors.toMultiValueMap(map -> map.isEmpty() ? null : map, adaptations));
    }

}
