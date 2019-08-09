package org.springframework.core.annotation;

import org.springframework.core.DecoratingProxy;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

public class AnnotationAwareOrderComparator extends OrderComparator {

    public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();

    @Override
    @Nullable
    protected Integer findOrder(Object obj) {
        Integer order = super.findOrder(obj);
        if (order != null) {
            return order;
        }
        return findOrderFromAnnotation(obj);
    }

    @Nullable
    private Integer findOrderFromAnnotation(Object obj) {
        AnnotatedElement element = (obj instanceof AnnotatedElement ? (AnnotatedElement) obj : obj.getClass());
        MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.EXHAUSTIVE);
        Integer order = OrderUtils.getOrderFromAnnotations(element, annotations);
        if (order == null && obj instanceof DecoratingProxy) {
            return findOrderFromAnnotation(((DecoratingProxy) obj).getDecoratedClass());
        }
        return order;
    }

    @Override
    @Nullable
    public Integer getPriority(Object obj) {
        if (obj instanceof Class) {
            return OrderUtils.getPriority((Class<?>) obj);
        }
        Integer priority = OrderUtils.getPriority(obj.getClass());
        if (priority == null && obj instanceof DecoratingProxy) {
            return getPriority(((DecoratingProxy) obj).getDecoratedClass());
        }
        return priority;
    }

    public static void sort(List<?> list) {
        if (list.size() > 1) {
            list.sort(INSTANCE);
        }
    }

    public static void sort(Object[] array) {
        if (array.length > 1) {
            Arrays.sort(array, INSTANCE);
        }
    }

    public static void sortIfNecessary(Object value) {
        if (value instanceof Object[]) {
            sort((Object[]) value);
        } else if (value instanceof List) {
            sort((List<?>) value);
        }
    }

}
