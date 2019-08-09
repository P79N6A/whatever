package org.springframework.core.annotation;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

public abstract class OrderUtils {

    private static final Object NOT_ANNOTATED = new Object();

    private static final String JAVAX_PRIORITY_ANNOTATION = "javax.annotation.Priority";

    private static final Map<AnnotatedElement, Object> orderCache = new ConcurrentReferenceHashMap<>(64);

    public static int getOrder(Class<?> type, int defaultOrder) {
        Integer order = getOrder(type);
        return (order != null ? order : defaultOrder);
    }

    @Nullable
    public static Integer getOrder(Class<?> type, @Nullable Integer defaultOrder) {
        Integer order = getOrder(type);
        return (order != null ? order : defaultOrder);
    }

    @Nullable
    public static Integer getOrder(Class<?> type) {
        return getOrderFromAnnotations(type, MergedAnnotations.from(type, SearchStrategy.EXHAUSTIVE));
    }

    @Nullable
    static Integer getOrderFromAnnotations(AnnotatedElement element, MergedAnnotations annotations) {
        if (!(element instanceof Class)) {
            return findOrder(annotations);
        }
        Object cached = orderCache.get(element);
        if (cached != null) {
            return (cached instanceof Integer ? (Integer) cached : null);
        }
        Integer result = findOrder(annotations);
        orderCache.put(element, result != null ? result : NOT_ANNOTATED);
        return result;
    }

    @Nullable
    private static Integer findOrder(MergedAnnotations annotations) {
        MergedAnnotation<Order> orderAnnotation = annotations.get(Order.class);
        if (orderAnnotation.isPresent()) {
            return orderAnnotation.getInt(MergedAnnotation.VALUE);
        }
        MergedAnnotation<?> priorityAnnotation = annotations.get(JAVAX_PRIORITY_ANNOTATION);
        if (priorityAnnotation.isPresent()) {
            return priorityAnnotation.getInt(MergedAnnotation.VALUE);
        }
        return null;
    }

    @Nullable
    public static Integer getPriority(Class<?> type) {
        return MergedAnnotations.from(type, SearchStrategy.EXHAUSTIVE).get(JAVAX_PRIORITY_ANNOTATION).getValue(MergedAnnotation.VALUE, Integer.class).orElse(null);
    }

}
