package org.springframework.beans;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {

    private final Map<String, FieldPropertyHandler> fieldMap = new HashMap<>();

    public DirectFieldAccessor(Object object) {
        super(object);
    }

    protected DirectFieldAccessor(Object object, String nestedPath, DirectFieldAccessor parent) {
        super(object, nestedPath, parent);
    }

    @Override
    @Nullable
    protected FieldPropertyHandler getLocalPropertyHandler(String propertyName) {
        FieldPropertyHandler propertyHandler = this.fieldMap.get(propertyName);
        if (propertyHandler == null) {
            Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
            if (field != null) {
                propertyHandler = new FieldPropertyHandler(field);
                this.fieldMap.put(propertyName, propertyHandler);
            }
        }
        return propertyHandler;
    }

    @Override
    protected DirectFieldAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
        return new DirectFieldAccessor(object, nestedPath, this);
    }

    @Override
    protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
        PropertyMatches matches = PropertyMatches.forField(propertyName, getRootClass());
        throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName, matches.buildErrorMessage(), matches.getPossibleMatches());
    }

    private class FieldPropertyHandler extends PropertyHandler {

        private final Field field;

        public FieldPropertyHandler(Field field) {
            super(field.getType(), true, true);
            this.field = field;
        }

        @Override
        public TypeDescriptor toTypeDescriptor() {
            return new TypeDescriptor(this.field);
        }

        @Override
        public ResolvableType getResolvableType() {
            return ResolvableType.forField(this.field);
        }

        @Override
        @Nullable
        public TypeDescriptor nested(int level) {
            return TypeDescriptor.nested(this.field, level);
        }

        @Override
        @Nullable
        public Object getValue() throws Exception {
            try {
                ReflectionUtils.makeAccessible(this.field);
                return this.field.get(getWrappedInstance());
            } catch (IllegalAccessException ex) {
                throw new InvalidPropertyException(getWrappedClass(), this.field.getName(), "Field is not accessible", ex);
            }
        }

        @Override
        public void setValue(@Nullable Object value) throws Exception {
            try {
                ReflectionUtils.makeAccessible(this.field);
                this.field.set(getWrappedInstance(), value);
            } catch (IllegalAccessException ex) {
                throw new InvalidPropertyException(getWrappedClass(), this.field.getName(), "Field is not accessible", ex);
            }
        }

    }

}
