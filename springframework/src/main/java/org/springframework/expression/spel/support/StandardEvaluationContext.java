package org.springframework.expression.spel.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandardEvaluationContext implements EvaluationContext {

    private TypedValue rootObject;

    @Nullable
    private volatile List<PropertyAccessor> propertyAccessors;

    @Nullable
    private volatile List<ConstructorResolver> constructorResolvers;

    @Nullable
    private volatile List<MethodResolver> methodResolvers;

    @Nullable
    private volatile ReflectiveMethodResolver reflectiveMethodResolver;

    @Nullable
    private BeanResolver beanResolver;

    @Nullable
    private TypeLocator typeLocator;

    @Nullable
    private TypeConverter typeConverter;

    private TypeComparator typeComparator = new StandardTypeComparator();

    private OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    public StandardEvaluationContext() {
        this.rootObject = TypedValue.NULL;
    }

    public StandardEvaluationContext(Object rootObject) {
        this.rootObject = new TypedValue(rootObject);
    }

    public void setRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
        this.rootObject = new TypedValue(rootObject, typeDescriptor);
    }

    public void setRootObject(@Nullable Object rootObject) {
        this.rootObject = (rootObject != null ? new TypedValue(rootObject) : TypedValue.NULL);
    }

    @Override
    public TypedValue getRootObject() {
        return this.rootObject;
    }

    public void setPropertyAccessors(List<PropertyAccessor> propertyAccessors) {
        this.propertyAccessors = propertyAccessors;
    }

    @Override
    public List<PropertyAccessor> getPropertyAccessors() {
        return initPropertyAccessors();
    }

    public void addPropertyAccessor(PropertyAccessor accessor) {
        addBeforeDefault(initPropertyAccessors(), accessor);
    }

    public boolean removePropertyAccessor(PropertyAccessor accessor) {
        return initPropertyAccessors().remove(accessor);
    }

    public void setConstructorResolvers(List<ConstructorResolver> constructorResolvers) {
        this.constructorResolvers = constructorResolvers;
    }

    @Override
    public List<ConstructorResolver> getConstructorResolvers() {
        return initConstructorResolvers();
    }

    public void addConstructorResolver(ConstructorResolver resolver) {
        addBeforeDefault(initConstructorResolvers(), resolver);
    }

    public boolean removeConstructorResolver(ConstructorResolver resolver) {
        return initConstructorResolvers().remove(resolver);
    }

    public void setMethodResolvers(List<MethodResolver> methodResolvers) {
        this.methodResolvers = methodResolvers;
    }

    @Override
    public List<MethodResolver> getMethodResolvers() {
        return initMethodResolvers();
    }

    public void addMethodResolver(MethodResolver resolver) {
        addBeforeDefault(initMethodResolvers(), resolver);
    }

    public boolean removeMethodResolver(MethodResolver methodResolver) {
        return initMethodResolvers().remove(methodResolver);
    }

    public void setBeanResolver(BeanResolver beanResolver) {
        this.beanResolver = beanResolver;
    }

    @Override
    @Nullable
    public BeanResolver getBeanResolver() {
        return this.beanResolver;
    }

    public void setTypeLocator(TypeLocator typeLocator) {
        Assert.notNull(typeLocator, "TypeLocator must not be null");
        this.typeLocator = typeLocator;
    }

    @Override
    public TypeLocator getTypeLocator() {
        if (this.typeLocator == null) {
            this.typeLocator = new StandardTypeLocator();
        }
        return this.typeLocator;
    }

    public void setTypeConverter(TypeConverter typeConverter) {
        Assert.notNull(typeConverter, "TypeConverter must not be null");
        this.typeConverter = typeConverter;
    }

    @Override
    public TypeConverter getTypeConverter() {
        if (this.typeConverter == null) {
            this.typeConverter = new StandardTypeConverter();
        }
        return this.typeConverter;
    }

    public void setTypeComparator(TypeComparator typeComparator) {
        Assert.notNull(typeComparator, "TypeComparator must not be null");
        this.typeComparator = typeComparator;
    }

    @Override
    public TypeComparator getTypeComparator() {
        return this.typeComparator;
    }

    public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
        Assert.notNull(operatorOverloader, "OperatorOverloader must not be null");
        this.operatorOverloader = operatorOverloader;
    }

    @Override
    public OperatorOverloader getOperatorOverloader() {
        return this.operatorOverloader;
    }

    @Override
    public void setVariable(@Nullable String name, @Nullable Object value) {
        // For backwards compatibility, we ignore null names here...
        // And since ConcurrentHashMap cannot store null values, we simply take null
        // as a remove from the Map (with the same result from lookupVariable below).
        if (name != null) {
            if (value != null) {
                this.variables.put(name, value);
            } else {
                this.variables.remove(name);
            }
        }
    }

    public void setVariables(Map<String, Object> variables) {
        variables.forEach(this::setVariable);
    }

    public void registerFunction(String name, Method method) {
        this.variables.put(name, method);
    }

    @Override
    @Nullable
    public Object lookupVariable(String name) {
        return this.variables.get(name);
    }

    public void registerMethodFilter(Class<?> type, MethodFilter filter) throws IllegalStateException {
        initMethodResolvers();
        ReflectiveMethodResolver resolver = this.reflectiveMethodResolver;
        if (resolver == null) {
            throw new IllegalStateException("Method filter cannot be set as the reflective method resolver is not in use");
        }
        resolver.registerMethodFilter(type, filter);
    }

    private List<PropertyAccessor> initPropertyAccessors() {
        List<PropertyAccessor> accessors = this.propertyAccessors;
        if (accessors == null) {
            accessors = new ArrayList<>(5);
            accessors.add(new ReflectivePropertyAccessor());
            this.propertyAccessors = accessors;
        }
        return accessors;
    }

    private List<ConstructorResolver> initConstructorResolvers() {
        List<ConstructorResolver> resolvers = this.constructorResolvers;
        if (resolvers == null) {
            resolvers = new ArrayList<>(1);
            resolvers.add(new ReflectiveConstructorResolver());
            this.constructorResolvers = resolvers;
        }
        return resolvers;
    }

    private List<MethodResolver> initMethodResolvers() {
        List<MethodResolver> resolvers = this.methodResolvers;
        if (resolvers == null) {
            resolvers = new ArrayList<>(1);
            this.reflectiveMethodResolver = new ReflectiveMethodResolver();
            resolvers.add(this.reflectiveMethodResolver);
            this.methodResolvers = resolvers;
        }
        return resolvers;
    }

    private static <T> void addBeforeDefault(List<T> resolvers, T resolver) {
        resolvers.add(resolvers.size() - 1, resolver);
    }

}
