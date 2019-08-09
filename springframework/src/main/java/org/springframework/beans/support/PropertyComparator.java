package org.springframework.beans.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PropertyComparator<T> implements Comparator<T> {

    protected final Log logger = LogFactory.getLog(getClass());

    private final SortDefinition sortDefinition;

    private final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(false);

    public PropertyComparator(SortDefinition sortDefinition) {
        this.sortDefinition = sortDefinition;
    }

    public PropertyComparator(String property, boolean ignoreCase, boolean ascending) {
        this.sortDefinition = new MutableSortDefinition(property, ignoreCase, ascending);
    }

    public final SortDefinition getSortDefinition() {
        return this.sortDefinition;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compare(T o1, T o2) {
        Object v1 = getPropertyValue(o1);
        Object v2 = getPropertyValue(o2);
        if (this.sortDefinition.isIgnoreCase() && (v1 instanceof String) && (v2 instanceof String)) {
            v1 = ((String) v1).toLowerCase();
            v2 = ((String) v2).toLowerCase();
        }
        int result;
        // Put an object with null property at the end of the sort result.
        try {
            if (v1 != null) {
                result = (v2 != null ? ((Comparable<Object>) v1).compareTo(v2) : -1);
            } else {
                result = (v2 != null ? 1 : 0);
            }
        } catch (RuntimeException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not sort objects [" + o1 + "] and [" + o2 + "]", ex);
            }
            return 0;
        }
        return (this.sortDefinition.isAscending() ? result : -result);
    }

    @Nullable
    private Object getPropertyValue(Object obj) {
        // If a nested property cannot be read, simply return null
        // (similar to JSTL EL). If the property doesn't exist in the
        // first place, let the exception through.
        try {
            this.beanWrapper.setWrappedInstance(obj);
            return this.beanWrapper.getPropertyValue(this.sortDefinition.getProperty());
        } catch (BeansException ex) {
            logger.debug("PropertyComparator could not access property - treating as null for sorting", ex);
            return null;
        }
    }

    public static void sort(List<?> source, SortDefinition sortDefinition) throws BeansException {
        if (StringUtils.hasText(sortDefinition.getProperty())) {
            source.sort(new PropertyComparator<>(sortDefinition));
        }
    }

    public static void sort(Object[] source, SortDefinition sortDefinition) throws BeansException {
        if (StringUtils.hasText(sortDefinition.getProperty())) {
            Arrays.sort(source, new PropertyComparator<>(sortDefinition));
        }
    }

}
