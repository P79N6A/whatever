package org.springframework.expression.spel.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

import java.util.*;

public final class SimpleEvaluationContext implements EvaluationContext {

    private static final TypeLocator typeNotFoundTypeLocator = typeName -> {
        throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
    };

    private final TypedValue rootObject;

    private final List<PropertyAccessor> propertyAccessors;

    private final List<MethodResolver> methodResolvers;

    private final TypeConverter typeConverter;

    private final TypeComparator typeComparator = new StandardTypeComparator();

    private final OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

    private final Map<String, Object> variables = new HashMap<>();

    private SimpleEvaluationContext(List<PropertyAccessor> accessors, List<MethodResolver> resolvers, @Nullable TypeConverter converter, @Nullable TypedValue rootObject) {
        this.propertyAccessors = accessors;
        this.methodResolvers = resolvers;
        this.typeConverter = (converter != null ? converter : new StandardTypeConverter());
        this.rootObject = (rootObject != null ? rootObject : TypedValue.NULL);
    }

    @Override
    public TypedValue getRootObject() {
        return this.rootObject;
    }

    @Override
    public List<PropertyAccessor> getPropertyAccessors() {
        return this.propertyAccessors;
    }

    @Override
    public List<ConstructorResolver> getConstructorResolvers() {
        return Collections.emptyList();
    }

    @Override
    public List<MethodResolver> getMethodResolvers() {
        return this.methodResolvers;
    }

    @Override
    @Nullable
    public BeanResolver getBeanResolver() {
        return null;
    }

    @Override
    public TypeLocator getTypeLocator() {
        return typeNotFoundTypeLocator;
    }

    @Override
    public TypeConverter getTypeConverter() {
        return this.typeConverter;
    }

    @Override
    public TypeComparator getTypeComparator() {
        return this.typeComparator;
    }

    @Override
    public OperatorOverloader getOperatorOverloader() {
        return this.operatorOverloader;
    }

    @Override
    public void setVariable(String name, @Nullable Object value) {
        this.variables.put(name, value);
    }

    @Override
    @Nullable
    public Object lookupVariable(String name) {
        return this.variables.get(name);
    }

    public static Builder forPropertyAccessors(PropertyAccessor... accessors) {
        for (PropertyAccessor accessor : accessors) {
            if (accessor.getClass() == ReflectivePropertyAccessor.class) {
                throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " + "ReflectivePropertyAccessor. Consider using DataBindingPropertyAccessor or a custom subclass.");
            }
        }
        return new Builder(accessors);
    }

    public static Builder forReadOnlyDataBinding() {
        return new Builder(DataBindingPropertyAccessor.forReadOnlyAccess());
    }

    public static Builder forReadWriteDataBinding() {
        return new Builder(DataBindingPropertyAccessor.forReadWriteAccess());
    }

    public static class Builder {

        private final List<PropertyAccessor> accessors;

        private List<MethodResolver> resolvers = Collections.emptyList();

        @Nullable
        private TypeConverter typeConverter;

        @Nullable
        private TypedValue rootObject;

        public Builder(PropertyAccessor... accessors) {
            this.accessors = Arrays.asList(accessors);
        }

        public Builder withMethodResolvers(MethodResolver... resolvers) {
            for (MethodResolver resolver : resolvers) {
                if (resolver.getClass() == ReflectiveMethodResolver.class) {
                    throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " + "ReflectiveMethodResolver. Consider using DataBindingMethodResolver or a custom subclass.");
                }
            }
            this.resolvers = Arrays.asList(resolvers);
            return this;
        }

        public Builder withInstanceMethods() {
            this.resolvers = Collections.singletonList(DataBindingMethodResolver.forInstanceMethodInvocation());
            return this;
        }

        public Builder withConversionService(ConversionService conversionService) {
            this.typeConverter = new StandardTypeConverter(conversionService);
            return this;
        }

        public Builder withTypeConverter(TypeConverter converter) {
            this.typeConverter = converter;
            return this;
        }

        public Builder withRootObject(Object rootObject) {
            this.rootObject = new TypedValue(rootObject);
            return this;
        }

        public Builder withTypedRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
            this.rootObject = new TypedValue(rootObject, typeDescriptor);
            return this;
        }

        public SimpleEvaluationContext build() {
            return new SimpleEvaluationContext(this.accessors, this.resolvers, this.typeConverter, this.rootObject);
        }

    }

}
