package org.springframework.beans.factory.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ListFactoryBean extends AbstractFactoryBean<List<Object>> {

    @Nullable
    private List<?> sourceList;

    @SuppressWarnings("rawtypes")
    @Nullable
    private Class<? extends List> targetListClass;

    public void setSourceList(List<?> sourceList) {
        this.sourceList = sourceList;
    }

    @SuppressWarnings("rawtypes")
    public void setTargetListClass(@Nullable Class<? extends List> targetListClass) {
        if (targetListClass == null) {
            throw new IllegalArgumentException("'targetListClass' must not be null");
        }
        if (!List.class.isAssignableFrom(targetListClass)) {
            throw new IllegalArgumentException("'targetListClass' must implement [java.util.List]");
        }
        this.targetListClass = targetListClass;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<List> getObjectType() {
        return List.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Object> createInstance() {
        if (this.sourceList == null) {
            throw new IllegalArgumentException("'sourceList' is required");
        }
        List<Object> result = null;
        if (this.targetListClass != null) {
            result = BeanUtils.instantiateClass(this.targetListClass);
        } else {
            result = new ArrayList<>(this.sourceList.size());
        }
        Class<?> valueType = null;
        if (this.targetListClass != null) {
            valueType = ResolvableType.forClass(this.targetListClass).asCollection().resolveGeneric();
        }
        if (valueType != null) {
            TypeConverter converter = getBeanTypeConverter();
            for (Object elem : this.sourceList) {
                result.add(converter.convertIfNecessary(elem, valueType));
            }
        } else {
            result.addAll(this.sourceList);
        }
        return result;
    }

}
