package org.springframework.core.annotation;

import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;

@FunctionalInterface
interface AnnotationsProcessor<C, R> {

    @Nullable
    default R doWithAggregate(C context, int aggregateIndex) {
        return null;
    }

    @Nullable
    R doWithAnnotations(C context, int aggregateIndex, @Nullable Object source, Annotation[] annotations);

    @Nullable
    default R finish(@Nullable R result) {
        return result;
    }

}
