package org.springframework.core.annotation;

import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {

    <A extends Annotation> boolean isPresent(Class<A> annotationType);

    boolean isPresent(String annotationType);

    <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType);

    boolean isDirectlyPresent(String annotationType);

    <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

    <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType, @Nullable Predicate<? super MergedAnnotation<A>> predicate);

    <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType, @Nullable Predicate<? super MergedAnnotation<A>> predicate, @Nullable MergedAnnotationSelector<A> selector);

    <A extends Annotation> MergedAnnotation<A> get(String annotationType);

    <A extends Annotation> MergedAnnotation<A> get(String annotationType, @Nullable Predicate<? super MergedAnnotation<A>> predicate);

    <A extends Annotation> MergedAnnotation<A> get(String annotationType, @Nullable Predicate<? super MergedAnnotation<A>> predicate, @Nullable MergedAnnotationSelector<A> selector);

    <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

    <A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

    Stream<MergedAnnotation<Annotation>> stream();

    static MergedAnnotations from(AnnotatedElement element) {
        return from(element, SearchStrategy.DIRECT);
    }

    static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy) {
        return from(element, searchStrategy, RepeatableContainers.standardRepeatables(), AnnotationFilter.PLAIN);
    }

    static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy, RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
        return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, annotationFilter);
    }

    static MergedAnnotations from(Annotation... annotations) {
        return from(null, annotations);
    }

    static MergedAnnotations from(@Nullable Object source, Annotation... annotations) {
        return from(source, annotations, RepeatableContainers.standardRepeatables(), AnnotationFilter.PLAIN);
    }

    static MergedAnnotations from(@Nullable Object source, Annotation[] annotations, RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
        return TypeMappedAnnotations.from(source, annotations, repeatableContainers, annotationFilter);
    }

    static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
        return MergedAnnotationsCollection.of(annotations);
    }

    enum SearchStrategy {

        DIRECT,

        INHERITED_ANNOTATIONS,

        SUPERCLASS,

        EXHAUSTIVE
    }

}
