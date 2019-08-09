package org.springframework.expression;

import org.springframework.lang.Nullable;

import java.util.List;

public interface EvaluationContext {

    TypedValue getRootObject();

    List<PropertyAccessor> getPropertyAccessors();

    List<ConstructorResolver> getConstructorResolvers();

    List<MethodResolver> getMethodResolvers();

    @Nullable
    BeanResolver getBeanResolver();

    TypeLocator getTypeLocator();

    TypeConverter getTypeConverter();

    TypeComparator getTypeComparator();

    OperatorOverloader getOperatorOverloader();

    void setVariable(String name, @Nullable Object value);

    @Nullable
    Object lookupVariable(String name);

}
