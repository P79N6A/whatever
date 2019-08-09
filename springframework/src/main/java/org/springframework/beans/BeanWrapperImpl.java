package org.springframework.beans;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.*;

public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

    @Nullable
    private CachedIntrospectionResults cachedIntrospectionResults;

    @Nullable
    private AccessControlContext acc;

    public BeanWrapperImpl() {
        this(true);
    }

    public BeanWrapperImpl(boolean registerDefaultEditors) {
        super(registerDefaultEditors);
    }

    public BeanWrapperImpl(Object object) {
        super(object);
    }

    public BeanWrapperImpl(Class<?> clazz) {
        super(clazz);
    }

    public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
        super(object, nestedPath, rootObject);
    }

    private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
        super(object, nestedPath, parent);
        setSecurityContext(parent.acc);
    }

    public void setBeanInstance(Object object) {
        this.wrappedObject = object;
        this.rootObject = object;
        this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
        setIntrospectionClass(object.getClass());
    }

    @Override
    public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
        super.setWrappedInstance(object, nestedPath, rootObject);
        setIntrospectionClass(getWrappedClass());
    }

    protected void setIntrospectionClass(Class<?> clazz) {
        if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
            this.cachedIntrospectionResults = null;
        }
    }

    private CachedIntrospectionResults getCachedIntrospectionResults() {
        if (this.cachedIntrospectionResults == null) {
            this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
        }
        return this.cachedIntrospectionResults;
    }

    public void setSecurityContext(@Nullable AccessControlContext acc) {
        this.acc = acc;
    }

    @Nullable
    public AccessControlContext getSecurityContext() {
        return this.acc;
    }

    @Nullable
    public Object convertForProperty(@Nullable Object value, String propertyName) throws TypeMismatchException {
        CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
        PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName, "No property '" + propertyName + "' found");
        }
        TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
        if (td == null) {
            td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
        }
        return convertForProperty(propertyName, null, value, td);
    }

    private Property property(PropertyDescriptor pd) {
        GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
        return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
    }

    @Override
    @Nullable
    protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
        PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
        return (pd != null ? new BeanPropertyHandler(pd) : null);
    }

    @Override
    protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
        return new BeanWrapperImpl(object, nestedPath, this);
    }

    @Override
    protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
        PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
        throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName, matches.buildErrorMessage(), matches.getPossibleMatches());
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return getCachedIntrospectionResults().getPropertyDescriptors();
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
        BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
        String finalPath = getFinalPath(nestedBw, propertyName);
        PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName, "No property '" + propertyName + "' found");
        }
        return pd;
    }

    private class BeanPropertyHandler extends PropertyHandler {

        private final PropertyDescriptor pd;

        public BeanPropertyHandler(PropertyDescriptor pd) {
            super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
            this.pd = pd;
        }

        @Override
        public ResolvableType getResolvableType() {
            return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
        }

        @Override
        public TypeDescriptor toTypeDescriptor() {
            return new TypeDescriptor(property(this.pd));
        }

        @Override
        @Nullable
        public TypeDescriptor nested(int level) {
            return TypeDescriptor.nested(property(this.pd), level);
        }

        @Override
        @Nullable
        public Object getValue() throws Exception {
            final Method readMethod = this.pd.getReadMethod();
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(readMethod);
                    return null;
                });
                try {
                    return AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> readMethod.invoke(getWrappedInstance(), (Object[]) null), acc);
                } catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            } else {
                ReflectionUtils.makeAccessible(readMethod);
                return readMethod.invoke(getWrappedInstance(), (Object[]) null);
            }
        }

        @Override
        public void setValue(final @Nullable Object value) throws Exception {
            final Method writeMethod = (this.pd instanceof GenericTypeAwarePropertyDescriptor ? ((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() : this.pd.getWriteMethod());
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(writeMethod);
                    return null;
                });
                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> writeMethod.invoke(getWrappedInstance(), value), acc);
                } catch (PrivilegedActionException ex) {
                    throw ex.getException();
                }
            } else {
                ReflectionUtils.makeAccessible(writeMethod);
                writeMethod.invoke(getWrappedInstance(), value);
            }
        }

    }

}
